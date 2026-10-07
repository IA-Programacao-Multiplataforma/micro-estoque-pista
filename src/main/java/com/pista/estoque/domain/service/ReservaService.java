package com.pista.estoque.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.ItemReserva;
import com.pista.estoque.domain.entity.Reserva;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.domain.entity.enumerable.StatusReserva;
import com.pista.estoque.domain.entity.enumerable.StatusSessao;
import com.pista.estoque.domain.evento.ReservaConfirmacaoFalhou;
import com.pista.estoque.domain.evento.ReservaConfirmada;
import com.pista.estoque.domain.evento.ReservaConfirmar;
import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.MensagemDuplicadaException;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;
import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.domain.repository.ReservaRepository;

public class ReservaService {

    private final EventoRepository eventos;
    private final ReservaRepository reservas;
    private final int maxIngressosPorCompra;
    private final Clock clock;
    private final UnidadeDeTrabalho unidade;
    private final PublicadorDeEventos publicador;
    private final EventosProcessados processados;

    public ReservaService(EventoRepository eventos, ReservaRepository reservas,
                          int maxIngressosPorCompra, Clock clock, UnidadeDeTrabalho unidade,
                          PublicadorDeEventos publicador, EventosProcessados processados) {
        this.eventos = eventos;
        this.reservas = reservas;
        this.maxIngressosPorCompra = maxIngressosPorCompra;
        this.clock = clock;
        this.unidade = unidade;
        this.publicador = publicador;
        this.processados = processados;
    }

    public Reserva criarOuSubstituir(String sessaoId, String usuarioId, Instant expiraEm,
                                     List<ItemSolicitado> solicitados) {
        Instant agora = Instant.now(clock);
        if (!expiraEm.isAfter(agora)) {
            throw new RegraDeNegocioException("A reserva precisa expirar em um momento futuro");
        }

        Map<String, Integer> quantidadePorSetor = agrupar(solicitados);

        Evento evento = eventos.buscarPorSessaoId(sessaoId)
                .orElseThrow(() -> new NaoEncontradoException("Sessão não encontrada"));
        Sessao sessao = evento.sessoes().stream()
                .filter(s -> s.id().equals(sessaoId))
                .findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Sessão não encontrada"));

        if (sessao.status(LocalDateTime.now(clock)) == StatusSessao.ESTREIA) {
            throw new ConflitoException("A venda desta sessão ainda não abriu");
        }

        List<ItemReserva> itens = new ArrayList<>();
        quantidadePorSetor.forEach((setorId, quantidade) -> {
            Setor setor = sessao.setores().stream()
                    .filter(s -> s.id().equals(setorId))
                    .findFirst()
                    .orElseThrow(() -> new NaoEncontradoException("Setor não encontrado"));
            itens.add(new ItemReserva(setor.id(), setor.nome(), quantidade, setor.preco()));
        });

        // Solta a reserva anterior antes de pedir a nova, senão os ingressos dela contariam contra o próprio cliente
        Reserva anterior = reservas.buscarAtivaPorSessaoEUsuario(sessaoId, usuarioId).orElse(null);
        if (anterior != null) {
            encerrar(anterior, StatusReserva.LIBERADA);
        }

        try {
            reservarTodos(evento.id(), sessaoId, itens);
        } catch (ConflitoException e) {
            // tenta devolver ao cliente a reserva que ele já tinha (melhor esforço)
            if (anterior != null) {
                restaurar(anterior);
            }
            throw e;
        }

        return reservas.salvar(new Reserva(UUID.randomUUID().toString(), sessaoId, evento.id(),
                usuarioId, StatusReserva.ATIVA, expiraEm, agora, itens));
    }

    public Reserva buscar(String id) {
        return reservas.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Reserva não encontrada"));
    }

    public void liberar(String id) {
        Reserva reserva = buscar(id);

        if (reserva.status() == StatusReserva.CONFIRMADA) {
            throw new ConflitoException("Reserva já confirmada não pode ser liberada");
        }
        if (reserva.status() == StatusReserva.ATIVA) {
            encerrar(reserva, StatusReserva.LIBERADA);
        }
    }

    // Numa transação só: registra o eventoId, muda reserva e estoque e grava a resposta; nada fica pela metade
    public void processarConfirmacao(String eventoId, ReservaConfirmar comando) {
        if (processados.jaProcessado(eventoId)) {
            return;
        }

        try {
            unidade.executar(() -> {
                processados.registrar(eventoId);
                decidirConfirmacao(comando);
                return null;
            });
        } catch (MensagemDuplicadaException duplicada) {
        }
    }

    private void decidirConfirmacao(ReservaConfirmar comando) {
        Reserva reserva = reservas.buscarPorId(comando.reservaId()).orElse(null);

        if (reserva == null) {
            falhou(comando, "Reserva não encontrada");
        } else if (reserva.status() == StatusReserva.CONFIRMADA) {
            confirmada(comando); // já estava confirmada: repete o aviso 
        } else if (reserva.status() != StatusReserva.ATIVA) {
            falhou(comando, "Reserva expirada ou liberada");
        } else if (reservas.transicionar(reserva.id(), StatusReserva.ATIVA, StatusReserva.CONFIRMADA)) {
            reserva.itens().forEach(item -> eventos.confirmarVenda(
                    reserva.eventoId(), reserva.sessaoId(), item.setorId(), item.quantidade()));
            confirmada(comando);
        } else {
            // Perdeu a corrida para a expiração/liberação entre a leitura e a troca de status
            falhou(comando, "Reserva expirada ou liberada");
        }
    }

    private void confirmada(ReservaConfirmar comando) {
        publicador.publicar(ReservaConfirmada.ROUTING_KEY,
                new ReservaConfirmada(comando.pedidoId(), comando.reservaId()));
    }

    private void falhou(ReservaConfirmar comando, String motivo) {
        publicador.publicar(ReservaConfirmacaoFalhou.ROUTING_KEY,
                new ReservaConfirmacaoFalhou(comando.pedidoId(), comando.reservaId(), motivo));
    }

    public int expirarVencidas() {
        List<Reserva> vencidas = reservas.listarAtivasVencidasAte(Instant.now(clock));
        vencidas.forEach(reserva -> encerrar(reserva, StatusReserva.EXPIRADA));
        return vencidas.size();
    }

    private Map<String, Integer> agrupar(List<ItemSolicitado> solicitados) {
        if (solicitados == null || solicitados.isEmpty()) {
            throw new RegraDeNegocioException("Informe pelo menos um item");
        }

        Map<String, Integer> porSetor = new LinkedHashMap<>();
        for (ItemSolicitado item : solicitados) {
            if (item.quantidade() <= 0) {
                throw new RegraDeNegocioException("Quantidade deve ser maior que zero");
            }
            porSetor.merge(item.setorId(), item.quantidade(), Integer::sum);
        }

        int total = porSetor.values().stream().mapToInt(Integer::intValue).sum();
        if (total > maxIngressosPorCompra) {
            throw new RegraDeNegocioException("Máximo de " + maxIngressosPorCompra + " ingressos por compra");
        }
        return porSetor;
    }

    //Se um setor não tiver ingressos, desfaz os que já tinham sido reservados
    private void reservarTodos(String eventoId, String sessaoId, List<ItemReserva> itens) {
        List<ItemReserva> feitos = new ArrayList<>();

        for (ItemReserva item : itens) {
            boolean reservou = eventos.reservarIngressos(eventoId, sessaoId, item.setorId(), item.quantidade());
            if (!reservou) {
                feitos.forEach(f -> eventos.devolverIngressos(eventoId, sessaoId, f.setorId(), f.quantidade()));
                throw new ConflitoException("Ingressos insuficientes no setor " + item.setorNome());
            }
            feitos.add(item);
        }
    }

    // Só quem consegue trocar o status devolve os ingressos: com expiração e liberação simultâneas, eles voltam uma única vez
    private void encerrar(Reserva reserva, StatusReserva destino) {
        unidade.executar(() -> {
            if (reservas.transicionar(reserva.id(), StatusReserva.ATIVA, destino)) {
                reserva.itens().forEach(item ->
                        eventos.devolverIngressos(reserva.eventoId(), reserva.sessaoId(), item.setorId(), item.quantidade()));
            }
            return null;
        });
    }

    // Reativa a MESMA reserva (mesmo id) para o id que o cliente guardou continuar válido
    private void restaurar(Reserva anterior) {
        try {
            reservarTodos(anterior.eventoId(), anterior.sessaoId(), anterior.itens());
            if (!reservas.transicionar(anterior.id(), StatusReserva.LIBERADA, StatusReserva.ATIVA)) {
                anterior.itens().forEach(item -> eventos.devolverIngressos(
                        anterior.eventoId(), anterior.sessaoId(), item.setorId(), item.quantidade()));
            }
        } catch (RuntimeException ignorado) {
        }
    }
}
