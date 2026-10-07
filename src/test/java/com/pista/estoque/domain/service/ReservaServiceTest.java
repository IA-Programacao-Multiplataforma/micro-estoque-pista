package com.pista.estoque.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Reserva;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.entity.enumerable.StatusReserva;
import com.pista.estoque.domain.evento.ReservaConfirmacaoFalhou;
import com.pista.estoque.domain.evento.ReservaConfirmada;
import com.pista.estoque.domain.evento.ReservaConfirmar;
import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.MensagemDuplicadaException;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;
import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.domain.repository.ReservaRepository;

class ReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Instant AGORA = Instant.parse("2026-10-10T15:00:00Z");

    private FakeEventoRepository eventos;
    private FakeReservaRepository reservas;
    private ReservaService service;
    private FakePublicador publicador;
    private FakeProcessados processados;

    @BeforeEach
    void preparar() {
        Setor pista = Setor.novo("pista", "Pista", new BigDecimal("100.00"), 10);
        Setor camarote = Setor.novo("camarote", "Camarote", new BigDecimal("300.00"), 2);
        LocalDateTime agoraLocal = LocalDateTime.ofInstant(AGORA, ZONA);
        Sessao aberta = new Sessao("ses", agoraLocal.plusDays(30), agoraLocal.minusDays(1), List.of(pista, camarote));
        Sessao estreia = new Sessao("ses-futura", agoraLocal.plusDays(60), agoraLocal.plusDays(5),
                List.of(Setor.novo("x", "X", BigDecimal.ONE, 5)));

        eventos = new FakeEventoRepository(new Evento("evt", "Show", null, Categoria.MUSICA, "L", "C", null, null,
                List.of(aberta, estreia)));
        reservas = new FakeReservaRepository();
        publicador = new FakePublicador();
        processados = new FakeProcessados();
        service = new ReservaService(eventos, reservas, 6, Clock.fixed(AGORA, ZONA), new DiretaSemTransacao(), publicador, processados);
    }

    private Instant daqui(long minutos) {
        return AGORA.plusSeconds(minutos * 60);
    }

    private int disponivel(String setorId) {
        return eventos.setor(setorId).disponivel();
    }

    @Test
    void reservaDiminuiODisponivelEGravaOPrecoDaEpoca() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 3)));

        assertThat(r.status()).isEqualTo(StatusReserva.ATIVA);
        assertThat(r.total()).isEqualByComparingTo("300.00");
        assertThat(disponivel("pista")).isEqualTo(7);
        assertThat(eventos.setor("pista").reservado()).isEqualTo(3);
    }

    @Test
    void itensRepetidosDoMesmoSetorSomam() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("pista", 2), new ItemSolicitado("pista", 1)));

        assertThat(r.itens()).hasSize(1);
        assertThat(r.itens().get(0).quantidade()).isEqualTo(3);
    }

    @Test
    void semIngressosSuficientesRecusa() {
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("camarote", 3))))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Ingressos insuficientes no setor Camarote");
        assertThat(disponivel("camarote")).isEqualTo(2);
    }

    @Test
    void reservaComVariosSetoresETudoOuNada() {
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("pista", 2), new ItemSolicitado("camarote", 3))))
                .isInstanceOf(ConflitoException.class);

        assertThat(disponivel("pista")).isEqualTo(10);
        assertThat(eventos.setor("pista").reservado()).isZero();
        assertThat(reservas.todas()).isEmpty();
    }

    @Test
    void novaReservaDoMesmoClienteSubstituiAAnterior() {
        service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 2)));
        service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 5)));

        assertThat(disponivel("pista")).isEqualTo(5);
        assertThat(eventos.setor("pista").reservado()).isEqualTo(5);
        assertThat(reservas.ativas()).hasSize(1);
    }

    @Test
    void substituirPodeUsarOsIngressosQueOProprioClienteJaSegurava() {
        service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 6)));
        service.criarOuSubstituir("ses", "bia@x.com", daqui(5), List.of(new ItemSolicitado("pista", 2)));

        Reserva nova = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 6)));

        assertThat(nova.itens().get(0).quantidade()).isEqualTo(6);
        assertThat(disponivel("pista")).isEqualTo(2);
    }

    @Test
    void substituicaoQueFalhaMantemASacolaAnterior() {
        Reserva original = service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("camarote", 1)));

        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("camarote", 2), new ItemSolicitado("pista", 99))))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("camarote", 3))))
                .isInstanceOf(ConflitoException.class);

        assertThat(reservas.ativas()).hasSize(1);
        assertThat(reservas.ativas().get(0).id()).isEqualTo(original.id());
        assertThat(reservas.ativas().get(0).itens().get(0).quantidade()).isEqualTo(1);
        assertThat(disponivel("camarote")).isEqualTo(1);
    }

    @Test
    void respeitaOMaximoPorCompra() {
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("pista", 7))))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Máximo de 6 ingressos por compra");
        assertThat(disponivel("pista")).isEqualTo(10);
    }

    @Test
    void validaEntradasInvalidas() {
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of()))
                .hasMessage("Informe pelo menos um item");
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("pista", 0)))).hasMessage("Quantidade deve ser maior que zero");
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", AGORA.minusSeconds(1),
                List.of(new ItemSolicitado("pista", 1)))).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void sessaoOuSetorInexistenteDao404() {
        assertThatThrownBy(() -> service.criarOuSubstituir("nao-existe", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("pista", 1)))).isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Sessão não encontrada");
        assertThatThrownBy(() -> service.criarOuSubstituir("ses", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("nao-existe", 1)))).isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Setor não encontrado");
    }

    @Test
    void naoReservaEmSessaoQueAindaNaoAbriuAVenda() {
        assertThatThrownBy(() -> service.criarOuSubstituir("ses-futura", "ana@x.com", daqui(5),
                List.of(new ItemSolicitado("x", 1))))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("A venda desta sessão ainda não abriu");
    }

    @Test
    void liberarDevolveOsIngressosUmaUnicaVez() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 4)));

        service.liberar(r.id());
        service.liberar(r.id());

        assertThat(disponivel("pista")).isEqualTo(10);
        assertThat(eventos.setor("pista").reservado()).isZero();
        assertThat(service.buscar(r.id()).status()).isEqualTo(StatusReserva.LIBERADA);
    }

    @Test
    void naoLiberaReservaConfirmada() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 1)));
        reservas.transicionar(r.id(), StatusReserva.ATIVA, StatusReserva.CONFIRMADA);

        assertThatThrownBy(() -> service.liberar(r.id())).isInstanceOf(ConflitoException.class);
        assertThat(disponivel("pista")).isEqualTo(9);
    }

    @Test
    void liberarReservaInexistenteDa404() {
        assertThatThrownBy(() -> service.liberar("nao-existe"))
                .isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Reserva não encontrada");
    }

    @Test
    void expirarDevolveSoAsVencidas() {
        Reserva curta = service.criarOuSubstituir("ses", "ana@x.com", daqui(1), List.of(new ItemSolicitado("pista", 2)));
        Reserva longa = service.criarOuSubstituir("ses", "bia@x.com", daqui(30), List.of(new ItemSolicitado("pista", 3)));
        assertThat(disponivel("pista")).isEqualTo(5);

        ReservaService depois = new ReservaService(eventos, reservas, 6, Clock.fixed(AGORA.plusSeconds(600), ZONA), new DiretaSemTransacao(), publicador, processados);
        int expiradas = depois.expirarVencidas();

        assertThat(expiradas).isEqualTo(1);
        assertThat(depois.buscar(curta.id()).status()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(depois.buscar(longa.id()).status()).isEqualTo(StatusReserva.ATIVA);
        assertThat(disponivel("pista")).isEqualTo(7);
        assertThat(depois.expirarVencidas()).isZero();
        assertThat(disponivel("pista")).isEqualTo(7);
    }

    private static ReservaConfirmar comando(Reserva r) {
        return new ReservaConfirmar("ped-1", r.id());
    }

    private ReservaService servicoDepoisDe(long segundos) {
        return new ReservaService(eventos, reservas, 6, Clock.fixed(AGORA.plusSeconds(segundos), ZONA),
                new DiretaSemTransacao(), publicador, processados);
    }

    @Test
    void confirmarTransformaReservaEmVendaEAnunciaPorMensagem() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 3)));

        service.processarConfirmacao("evt-1", comando(r));

        assertThat(service.buscar(r.id()).status()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(eventos.setor("pista").reservado()).isZero();
        assertThat(eventos.setor("pista").vendido()).isEqualTo(3);
        assertThat(disponivel("pista")).isEqualTo(7);
        assertThat(publicador.chaves()).containsExactly("reserva.confirmada");
        assertThat(publicador.ultimaComoConfirmada().pedidoId()).isEqualTo("ped-1");
    }

    @Test
    void mesmaMensagemEntregueDuasVezesVendeUmaVezSo() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 3)));

        service.processarConfirmacao("evt-1", comando(r));
        service.processarConfirmacao("evt-1", comando(r));

        assertThat(eventos.setor("pista").vendido()).isEqualTo(3);
        assertThat(disponivel("pista")).isEqualTo(7);
        assertThat(publicador.chaves()).hasSize(1);
    }

    @Test
    void mensagemDiferenteParaReservaJaConfirmadaRepeteOAvisoSemVenderEmDobro() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 3)));

        service.processarConfirmacao("evt-1", comando(r));
        service.processarConfirmacao("evt-2", comando(r));

        assertThat(eventos.setor("pista").vendido()).isEqualTo(3);
        assertThat(publicador.chaves()).containsExactly("reserva.confirmada", "reserva.confirmada");
    }

    @Test
    void duasEntregasSimultaneasDaMesmaMensagemConfirmamUmaVezSo() throws Exception {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 3)));
        // Simula a corrida: as duas passam pela checagem rápida "já processado?" antes de qualquer registro
        processados.ignorarChecagemRapida = true;

        Thread a = new Thread(() -> service.processarConfirmacao("evt-1", comando(r)));
        Thread b = new Thread(() -> service.processarConfirmacao("evt-1", comando(r)));
        a.start();
        b.start();
        a.join();
        b.join();

        assertThat(eventos.setor("pista").vendido()).isEqualTo(3);
        assertThat(publicador.chaves()).hasSize(1);
    }

    @Test
    void reservaLiberadaOuExpiradaAnunciaFalhaParaOPedidoEstornar() {
        Reserva liberada = service.criarOuSubstituir("ses", "ana@x.com", daqui(5), List.of(new ItemSolicitado("pista", 1)));
        service.liberar(liberada.id());
        Reserva curta = service.criarOuSubstituir("ses", "bia@x.com", daqui(1), List.of(new ItemSolicitado("pista", 1)));
        servicoDepoisDe(600).expirarVencidas();

        service.processarConfirmacao("evt-1", comando(liberada));
        service.processarConfirmacao("evt-2", comando(curta));

        assertThat(publicador.chaves()).containsExactly("reserva.confirmacao-falhou", "reserva.confirmacao-falhou");
        assertThat(publicador.ultimaComoFalha().motivo()).isEqualTo("Reserva expirada ou liberada");
        assertThat(eventos.setor("pista").vendido()).isZero();
        assertThat(disponivel("pista")).isEqualTo(10);
    }

    @Test
    void reservaInexistenteAnunciaFalha() {
        service.processarConfirmacao("evt-1", new ReservaConfirmar("ped-1", "nao-existe"));

        assertThat(publicador.chaves()).containsExactly("reserva.confirmacao-falhou");
        assertThat(publicador.ultimaComoFalha().motivo()).isEqualTo("Reserva não encontrada");
    }

    @Test
    void reservaConfirmadaNaoEMaisExpiradaNemLiberada() {
        Reserva r = service.criarOuSubstituir("ses", "ana@x.com", daqui(1), List.of(new ItemSolicitado("pista", 2)));
        service.processarConfirmacao("evt-1", comando(r));

        servicoDepoisDe(600).expirarVencidas();
        assertThatThrownBy(() -> service.liberar(r.id())).isInstanceOf(ConflitoException.class);

        assertThat(service.buscar(r.id()).status()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(eventos.setor("pista").vendido()).isEqualTo(2);
        assertThat(disponivel("pista")).isEqualTo(8);
    }

    static class FakePublicador implements PublicadorDeEventos {
        private final List<Object[]> mensagens = new ArrayList<>();

        @Override
        public synchronized void publicar(String routingKey, Object dados) {
            mensagens.add(new Object[] { routingKey, dados });
        }

        synchronized List<String> chaves() {
            return mensagens.stream().map(m -> (String) m[0]).toList();
        }

        synchronized ReservaConfirmada ultimaComoConfirmada() {
            return (ReservaConfirmada) mensagens.get(mensagens.size() - 1)[1];
        }

        synchronized ReservaConfirmacaoFalhou ultimaComoFalha() {
            return (ReservaConfirmacaoFalhou) mensagens.get(mensagens.size() - 1)[1];
        }
    }

    static class FakeProcessados implements EventosProcessados {
        private final java.util.Set<String> ids = new java.util.HashSet<>();
        volatile boolean ignorarChecagemRapida = false;

        @Override
        public synchronized boolean jaProcessado(String eventoId) {
            return !ignorarChecagemRapida && ids.contains(eventoId);
        }

        @Override
        public synchronized void registrar(String eventoId) {
            if (!ids.add(eventoId)) throw new MensagemDuplicadaException(eventoId);
        }
    }

    static class DiretaSemTransacao implements UnidadeDeTrabalho {
        @Override
        public <T> T executar(java.util.function.Supplier<T> trabalho) {
            return trabalho.get();
        }
    }

    static class FakeEventoRepository implements EventoRepository {
        private Evento evento;

        FakeEventoRepository(Evento evento) {
            this.evento = evento;
        }

        Setor setor(String setorId) {
            return evento.sessoes().stream().flatMap(s -> s.setores().stream())
                    .filter(s -> s.id().equals(setorId)).findFirst().orElseThrow();
        }

        private synchronized void alterar(String sessaoId, String setorId, int delta) {
            List<Sessao> novas = evento.sessoes().stream().map(sessao -> {
                if (!sessao.id().equals(sessaoId)) return sessao;
                List<Setor> setores = sessao.setores().stream().map(s -> s.id().equals(setorId)
                        ? new Setor(s.id(), s.nome(), s.preco(), s.capacidade(),
                                s.disponivel() - delta, s.reservado() + delta, s.vendido())
                        : s).toList();
                return new Sessao(sessao.id(), sessao.dataEvento(), sessao.inicioVenda(), setores);
            }).toList();
            evento = new Evento(evento.id(), evento.titulo(), evento.descricao(), evento.categoria(), evento.local(),
                    evento.cidade(), evento.imagemUrl(), evento.imagemAlt(), novas);
        }

        @Override
        public synchronized boolean reservarIngressos(String eventoId, String sessaoId, String setorId, int qtd) {
            Setor s = setor(setorId);
            if (s.disponivel() < qtd) return false;
            alterar(sessaoId, setorId, qtd);
            return true;
        }

        @Override
        public synchronized void devolverIngressos(String eventoId, String sessaoId, String setorId, int qtd) {
            alterar(sessaoId, setorId, -qtd);
        }

        @Override
        public synchronized void confirmarVenda(String eventoId, String sessaoId, String setorId, int qtd) {
            List<Sessao> novas = evento.sessoes().stream().map(sessao -> {
                if (!sessao.id().equals(sessaoId)) return sessao;
                List<Setor> setores = sessao.setores().stream().map(s -> s.id().equals(setorId)
                        ? new Setor(s.id(), s.nome(), s.preco(), s.capacidade(),
                                s.disponivel(), s.reservado() - qtd, s.vendido() + qtd)
                        : s).toList();
                return new Sessao(sessao.id(), sessao.dataEvento(), sessao.inicioVenda(), setores);
            }).toList();
            evento = new Evento(evento.id(), evento.titulo(), evento.descricao(), evento.categoria(), evento.local(),
                    evento.cidade(), evento.imagemUrl(), evento.imagemAlt(), novas);
        }

        @Override
        public Optional<Evento> buscarPorSessaoId(String sessaoId) {
            return evento.sessoes().stream().anyMatch(s -> s.id().equals(sessaoId))
                    ? Optional.of(evento) : Optional.empty();
        }

        @Override public Evento salvar(Evento e) { throw new UnsupportedOperationException(); }
        @Override public void deletar(String id) { throw new UnsupportedOperationException(); }
        @Override public Optional<Evento> buscarPorId(String id) { return Optional.of(evento); }
        @Override public List<Evento> listar() { return List.of(evento); }
    }

    static class FakeReservaRepository implements ReservaRepository {
        private final Map<String, Reserva> dados = new LinkedHashMap<>();

        List<Reserva> todas() {
            return new ArrayList<>(dados.values());
        }

        List<Reserva> ativas() {
            return dados.values().stream().filter(r -> r.status() == StatusReserva.ATIVA).toList();
        }

        @Override
        public synchronized Reserva salvar(Reserva r) {
            dados.put(r.id(), r);
            return r;
        }

        @Override
        public Optional<Reserva> buscarPorId(String id) {
            return Optional.ofNullable(dados.get(id));
        }

        @Override
        public Optional<Reserva> buscarAtivaPorSessaoEUsuario(String sessaoId, String usuarioId) {
            return ativas().stream()
                    .filter(r -> r.sessaoId().equals(sessaoId) && r.usuarioId().equals(usuarioId)).findFirst();
        }

        @Override
        public List<Reserva> listarAtivasVencidasAte(Instant limite) {
            return ativas().stream().filter(r -> r.expiraEm().isBefore(limite)).toList();
        }

        @Override
        public synchronized boolean transicionar(String id, StatusReserva de, StatusReserva para) {
            Reserva r = dados.get(id);
            if (r == null || r.status() != de) return false;
            dados.put(id, new Reserva(r.id(), r.sessaoId(), r.eventoId(), r.usuarioId(), para,
                    r.expiraEm(), r.criadaEm(), r.itens()));
            return true;
        }
    }
}
