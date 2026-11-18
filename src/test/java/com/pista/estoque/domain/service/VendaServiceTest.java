package com.pista.estoque.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.evento.PedidoCancelado;
import com.pista.estoque.domain.evento.PedidoCriado;
import com.pista.estoque.domain.evento.PedidoRealizado;
import com.pista.estoque.domain.repository.EventoRepository;

class VendaServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 10, 12, 0);

    private EventoEmMemoria eventos;
    private List<Object[]> enviadas;
    private VendaService service;

    @BeforeEach
    void preparar() {
        Setor pista = new Setor("pista", "Pista", new BigDecimal("100.00"), 5, 5, 0);
        Setor camarote = new Setor("camarote", "Camarote", new BigDecimal("300.00"), 1, 1, 0);
        Sessao sessao = new Sessao("s1", AGORA.plusDays(30), AGORA.minusDays(1), List.of(pista, camarote));
        eventos = new EventoEmMemoria(new Evento("e1", "Show", null, Categoria.MUSICA, "Local", "Cidade",
                null, null, List.of(sessao)));

        enviadas = new ArrayList<>();
        PublicadorDeEventos publicador = (routingKey, dados) -> enviadas.add(new Object[] {routingKey, dados});
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        service = new VendaService(eventos, eventos, publicador, 6, clock);
    }

    @Test
    void comIngressosDisponiveisDaBaixaEMandaRealizado() {
        service.processar(pedido("p1", item("pista", 2)));

        assertThat(setor("pista").disponivel()).isEqualTo(3);
        assertThat(setor("pista").vendido()).isEqualTo(2);
        assertThat(enviadas.get(0)[0]).isEqualTo(PedidoRealizado.ROUTING_KEY);
        PedidoRealizado realizado = (PedidoRealizado) enviadas.get(0)[1];
        assertThat(realizado.pedidoId()).isEqualTo("p1");
        assertThat(realizado.itens().get(0).precoUnitario()).isEqualByComparingTo("100.00");
    }

    @Test
    void semIngressosSuficientesMandaCanceladoENaoMexeNoEstoque() {
        service.processar(pedido("p1", item("pista", 2), item("camarote", 2)));

        // A pista chegou a ter baixa, mas o rollback desfez porque faltou camarote
        assertThat(setor("pista").disponivel()).isEqualTo(5);
        assertThat(setor("camarote").disponivel()).isEqualTo(1);
        assertThat(enviadas.get(0)[0]).isEqualTo(PedidoCancelado.ROUTING_KEY);
        assertThat(((PedidoCancelado) enviadas.get(0)[1]).motivo()).contains("Camarote");
    }

    @Test
    void nuncaVendeMaisDoQueExiste() {
        for (int i = 0; i < 10; i++) {
            service.processar(pedido("p" + i, item("pista", 1)));
        }

        assertThat(setor("pista").vendido()).isEqualTo(5);
        assertThat(setor("pista").disponivel()).isZero();
        assertThat(enviadas.stream().filter(m -> m[0].equals(PedidoRealizado.ROUTING_KEY))).hasSize(5);
        assertThat(enviadas.stream().filter(m -> m[0].equals(PedidoCancelado.ROUTING_KEY))).hasSize(5);
    }

    @Test
    void sessaoInexistenteMandaCancelado() {
        service.processar(new PedidoCriado("p1", "nao-existe", List.of(item("pista", 1))));

        assertThat(enviadas.get(0)[0]).isEqualTo(PedidoCancelado.ROUTING_KEY);
    }

    @Test
    void acimaDoLimitePorCompraMandaCancelado() {
        service.processar(pedido("p1", item("pista", 7)));

        assertThat(enviadas.get(0)[0]).isEqualTo(PedidoCancelado.ROUTING_KEY);
        assertThat(setor("pista").disponivel()).isEqualTo(5);
    }

    private static PedidoCriado pedido(String id, PedidoCriado.Item... itens) {
        return new PedidoCriado(id, "s1", List.of(itens));
    }

    private static PedidoCriado.Item item(String setorId, int quantidade) {
        return new PedidoCriado.Item(setorId, quantidade);
    }

    private Setor setor(String id) {
        return eventos.evento.sessoes().get(0).setores().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }

    // Banco falso: guarda um evento e simula a transação (se der exceção, volta ao estado anterior)
    static class EventoEmMemoria implements EventoRepository, UnidadeDeTrabalho {
        Evento evento;

        EventoEmMemoria(Evento evento) {
            this.evento = evento;
        }

        @Override
        public <T> T executar(Supplier<T> trabalho) {
            Evento antes = evento;
            try {
                return trabalho.get();
            } catch (RuntimeException e) {
                evento = antes; // rollback
                throw e;
            }
        }

        @Override
        public boolean travarEvento(String eventoId) {
            return evento.id().equals(eventoId);
        }

        @Override
        public boolean travarParaVenda(String sessaoId) {
            return buscarPorSessaoId(sessaoId).isPresent();
        }

        @Override
        public void darBaixa(String eventoId, String sessaoId, String setorId, int quantidade) {
            List<Sessao> sessoes = evento.sessoes().stream().map(s -> !s.id().equals(sessaoId) ? s
                    : new Sessao(s.id(), s.dataEvento(), s.inicioVenda(), s.setores().stream()
                            .map(t -> !t.id().equals(setorId) ? t : new Setor(t.id(), t.nome(), t.preco(),
                                    t.capacidade(), t.disponivel() - quantidade, t.vendido() + quantidade))
                            .toList()))
                    .toList();
            evento = new Evento(evento.id(), evento.titulo(), evento.descricao(), evento.categoria(), evento.local(),
                    evento.cidade(), evento.imagemUrl(), evento.imagemAlt(), sessoes);
        }

        @Override
        public Optional<Evento> buscarPorSessaoId(String sessaoId) {
            return evento.sessoes().stream().anyMatch(s -> s.id().equals(sessaoId)) ? Optional.of(evento) : Optional.empty();
        }

        @Override
        public Evento salvar(Evento e) {
            evento = e;
            return e;
        }

        @Override
        public void deletar(String id) {
        }

        @Override
        public Optional<Evento> buscarPorId(String id) {
            return Optional.of(evento);
        }

        @Override
        public List<Evento> listar() {
            return List.of(evento);
        }
    }
}
