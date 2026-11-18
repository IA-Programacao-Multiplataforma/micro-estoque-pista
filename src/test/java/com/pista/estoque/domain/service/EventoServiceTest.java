package com.pista.estoque.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;
import com.pista.estoque.domain.repository.EventoRepository;

class EventoServiceTest {

    private static final LocalDateTime DATA = LocalDateTime.of(2027, 3, 10, 21, 0);
    private static final LocalDateTime VENDA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private BancoFalso banco;
    private EventoService service;

    private static Setor setor(String id, String nome, int capacidade, int vendido) {
        return new Setor(id, nome, BigDecimal.valueOf(100), capacidade, capacidade - vendido, vendido);
    }

    @BeforeEach
    void preparar() {
        Sessao s1 = new Sessao("s1", DATA, VENDA, List.of(setor("a", "Pista", 100, 0), setor("b", "Camarote", 20, 5)));
        Sessao s2 = new Sessao("s2", DATA.plusDays(1), VENDA, List.of(setor("c", "Pista", 100, 0)));
        banco = new BancoFalso(new Evento("e1", "U2", "desc", Categoria.MUSICA, "MorumBIS", "São Paulo", "http://img", "alt",
                List.of(s1, s2)));
        service = new EventoService(banco, banco);
    }

    // ---------------------------------------------------------------- dados do evento

    @Test
    void editaOsDadosDoEventoSemMexerNasDatas() {
        Evento e = service.atualizarEvento("e1", "  U2 Tour  ", "nova descrição", Categoria.FESTIVAL, "Estádio", "Rio", "http://nova", "alt2");

        assertThat(e.titulo()).isEqualTo("U2 Tour");
        assertThat(e.categoria()).isEqualTo(Categoria.FESTIVAL);
        assertThat(e.local()).isEqualTo("Estádio");
        assertThat(e.imagemUrl()).isEqualTo("http://nova");
        assertThat(e.sessoes()).hasSize(2);
        assertThat(banco.evento.titulo()).isEqualTo("U2 Tour");
    }

    @Test
    void tituloLocalECidadeNaoPodemFicarEmBranco() {
        assertThatThrownBy(() -> service.atualizarEvento("e1", " ", null, Categoria.MUSICA, "L", "C", null, null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.atualizarEvento("e1", "T", null, Categoria.MUSICA, "", "C", null, null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.atualizarEvento("e1", "T", null, Categoria.MUSICA, "L", null, null, null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(banco.evento.titulo()).isEqualTo("U2");
    }

    @Test
    void eventoInexistenteDaNaoEncontrado() {
        assertThatThrownBy(() -> service.atualizarEvento("nao-existe", "T", null, Categoria.MUSICA, "L", "C", null, null))
                .isInstanceOf(NaoEncontradoException.class);
    }

    // ---------------------------------------------------------------- datas

    @Test
    void trocaADataEOInicioDaVenda() {
        Evento e = service.atualizarSessao("e1", "s2", DATA.plusDays(10), VENDA.plusDays(1));

        Sessao s2 = e.sessoes().get(1);
        assertThat(s2.dataEvento()).isEqualTo(DATA.plusDays(10));
        assertThat(s2.inicioVenda()).isEqualTo(VENDA.plusDays(1));
        assertThat(s2.setores()).hasSize(1);   // os setores ficam como estavam
    }

    @Test
    void inicioDaVendaDepoisDoEventoEhRecusado() {
        assertThatThrownBy(() -> service.atualizarSessao("e1", "s2", DATA, DATA.plusDays(1)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void excluiUmaDataSemVendas() {
        Evento e = service.excluirSessao("e1", "s2");

        assertThat(e.sessoes()).extracting(Sessao::id).containsExactly("s1");
    }

    @Test
    void naoExcluiDataComIngressosVendidos() {
        assertThatThrownBy(() -> service.excluirSessao("e1", "s1")).isInstanceOf(ConflitoException.class);
        assertThat(banco.evento.sessoes()).hasSize(2);
    }

    @Test
    void naoExcluiAUltimaData() {
        service.excluirSessao("e1", "s2");
        // sobrou só a s1 (com vendas): primeiro libera as vendas no banco falso para testar a regra da última data
        banco.evento = new Evento("e1", "U2", null, Categoria.MUSICA, "L", "C", null, null,
                List.of(new Sessao("s1", DATA, VENDA, List.of(setor("a", "Pista", 100, 0)))));

        assertThatThrownBy(() -> service.excluirSessao("e1", "s1")).isInstanceOf(RegraDeNegocioException.class);
    }

    // ---------------------------------------------------------------- setores

    @Test
    void acrescentaUmSetorNaData() {
        Evento e = service.adicionarSetor("e1", "s2", "  Gold Circle ", BigDecimal.valueOf(1750), 400);

        List<Setor> setores = e.sessoes().get(1).setores();
        assertThat(setores).hasSize(2);
        Setor novo = setores.get(1);
        assertThat(novo.nome()).isEqualTo("Gold Circle");
        assertThat(novo.capacidade()).isEqualTo(400);
        assertThat(novo.disponivel()).isEqualTo(400);
        assertThat(novo.vendido()).isZero();
        assertThat(novo.id()).isNotBlank();
    }

    @Test
    void naoAcrescentaSetorComNomeRepetidoNaMesmaData() {
        assertThatThrownBy(() -> service.adicionarSetor("e1", "s1", "pista", BigDecimal.TEN, 10))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void renomeiaEMudaPrecoECapacidadeDoSetor() {
        Evento e = service.atualizarSetor("e1", "s1", "b", "Camarote VIP", 30, BigDecimal.valueOf(250));

        Setor b = e.sessoes().get(0).setores().get(1);
        assertThat(b.nome()).isEqualTo("Camarote VIP");
        assertThat(b.preco()).isEqualByComparingTo("250");
        assertThat(b.capacidade()).isEqualTo(30);
        assertThat(b.vendido()).isEqualTo(5);       // as vendas não mudam
        assertThat(b.disponivel()).isEqualTo(25);   // capacidade - vendido
    }

    @Test
    void soMudaOQueFoiInformado() {
        Evento e = service.atualizarSetor("e1", "s1", "a", null, null, BigDecimal.valueOf(55));

        Setor a = e.sessoes().get(0).setores().get(0);
        assertThat(a.nome()).isEqualTo("Pista");
        assertThat(a.capacidade()).isEqualTo(100);
        assertThat(a.preco()).isEqualByComparingTo("55");
    }

    @Test
    void naoRenomeiaParaUmNomeQueJaExisteNaData() {
        assertThatThrownBy(() -> service.atualizarSetor("e1", "s1", "b", "PISTA", null, null))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void naoReduzACapacidadeAbaixoDoVendido() {
        assertThatThrownBy(() -> service.atualizarSetor("e1", "s1", "b", null, 4, null))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void naoAceitaAjusteSemNadaParaMudar() {
        assertThatThrownBy(() -> service.atualizarSetor("e1", "s1", "a", null, null, null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void excluiSetorSemVendas() {
        Evento e = service.excluirSetor("e1", "s1", "a");

        assertThat(e.sessoes().get(0).setores()).extracting(Setor::id).containsExactly("b");
    }

    @Test
    void naoExcluiSetorComIngressosVendidos() {
        assertThatThrownBy(() -> service.excluirSetor("e1", "s1", "b")).isInstanceOf(ConflitoException.class);
    }

    @Test
    void naoExcluiOUnicoSetorDaData() {
        assertThatThrownBy(() -> service.excluirSetor("e1", "s2", "c")).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void setorOuDataInexistenteDaNaoEncontrado() {
        assertThatThrownBy(() -> service.atualizarSetor("e1", "s1", "zzz", "X", null, null)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> service.excluirSetor("e1", "zzz", "a")).isInstanceOf(NaoEncontradoException.class);
    }

    // ---------------------------------------------------------------- exclusão do evento e trava

    @Test
    void naoExcluiEventoComVendasEExcluiSemVendas() {
        assertThatThrownBy(() -> service.excluirEvento("e1")).isInstanceOf(ConflitoException.class);
        assertThat(banco.excluido).isFalse();

        banco.evento = new Evento("e1", "U2", null, Categoria.MUSICA, "L", "C", null, null,
                List.of(new Sessao("s1", DATA, VENDA, List.of(setor("a", "Pista", 100, 0)))));
        service.excluirEvento("e1");

        assertThat(banco.excluido).isTrue();
    }

    @Test
    void todaAlteracaoTravaOEventoAntesDeGravar() {
        service.atualizarSetor("e1", "s1", "a", null, 120, null);
        service.excluirSessao("e1", "s2");

        assertThat(banco.travas).isEqualTo(2);
    }

    @Test
    void erroNaAlteracaoNaoGravaNada() {
        Evento antes = banco.evento;

        assertThatThrownBy(() -> service.atualizarSetor("e1", "s1", "b", null, 1, null)).isInstanceOf(ConflitoException.class);

        assertThat(banco.evento).isEqualTo(antes);
    }

    // Banco falso: guarda um evento, conta as travas e desfaz a transação se der erro
    static class BancoFalso implements EventoRepository, UnidadeDeTrabalho {
        Evento evento;
        int travas;
        boolean excluido;

        BancoFalso(Evento evento) {
            this.evento = evento;
        }

        @Override
        public <T> T executar(Supplier<T> trabalho) {
            Evento antes = evento;
            try {
                return trabalho.get();
            } catch (RuntimeException e) {
                evento = antes;
                throw e;
            }
        }

        @Override
        public boolean travarEvento(String eventoId) {
            travas++;
            return evento.id().equals(eventoId);
        }

        @Override
        public Evento salvar(Evento e) {
            evento = e;
            return e;
        }

        @Override
        public void deletar(String id) {
            excluido = true;
        }

        @Override
        public Optional<Evento> buscarPorId(String id) {
            return evento.id().equals(id) ? Optional.of(evento) : Optional.empty();
        }

        @Override
        public Optional<Evento> buscarPorSessaoId(String sessaoId) {
            return Optional.empty();
        }

        @Override
        public List<Evento> listar() {
            return List.of(evento);
        }

        @Override
        public boolean travarParaVenda(String sessaoId) {
            return false;
        }

        @Override
        public void darBaixa(String eventoId, String sessaoId, String setorId, int quantidade) {
        }
    }
}
