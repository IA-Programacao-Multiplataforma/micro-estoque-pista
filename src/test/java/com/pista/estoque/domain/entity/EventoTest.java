package com.pista.estoque.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.entity.enumerable.StatusSessao;
import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;

class EventoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 10, 12, 0);

    private Setor setor(int capacidade, int disponivel, int reservado, int vendido) {
        return new Setor("st", "Pista", BigDecimal.TEN, capacidade, disponivel, reservado, vendido);
    }

    private Sessao sessao(LocalDateTime inicioVenda, Setor... setores) {
        return new Sessao("ses", inicioVenda.plusDays(30), inicioVenda, List.of(setores));
    }

    private Evento evento(Sessao... sessoes) {
        return new Evento("evt", "Show", null, Categoria.MUSICA, "Local", "Cidade", null, null, List.of(sessoes));
    }

    @Test
    void statusEstreiaAntesDaAberturaDaVenda() {
        Sessao s = sessao(AGORA.plusDays(1), setor(100, 100, 0, 0));
        assertThat(s.status(AGORA)).isEqualTo(StatusSessao.ESTREIA);
    }

    @Test
    void statusDisponivelQuandoVendaAbertaEHaIngressos() {
        Sessao s = sessao(AGORA.minusDays(1), setor(100, 10, 50, 40));
        assertThat(s.status(AGORA)).isEqualTo(StatusSessao.DISPONIVEL);
    }

    @Test
    void statusEsgotadoQuandoNaoSobraNenhumIngresso() {
        Sessao s = sessao(AGORA.minusDays(1), setor(100, 0, 20, 80));
        assertThat(s.status(AGORA)).isEqualTo(StatusSessao.ESGOTADO);
    }

    @Test
    void inicioDaVendaNaoPodeSerDepoisDoEvento() {
        assertThatThrownBy(() -> new Sessao("s", AGORA, AGORA.plusDays(1), List.of(setor(1, 1, 0, 0))))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void ajusteDeCapacidadeRecalculaODisponivel() {
        Setor ajustado = setor(100, 10, 50, 40).ajustar(200, null);

        assertThat(ajustado.capacidade()).isEqualTo(200);
        assertThat(ajustado.disponivel()).isEqualTo(110);
        assertThat(ajustado.capacidade())
                .isEqualTo(ajustado.disponivel() + ajustado.reservado() + ajustado.vendido());
    }

    @Test
    void naoPermiteReduzirCapacidadeAbaixoDoReservadoMaisVendido() {
        Setor s = setor(100, 10, 50, 40);

        assertThatThrownBy(() -> s.ajustar(89, null)).isInstanceOf(ConflitoException.class);
        assertThat(s.ajustar(90, null).disponivel()).isZero();
    }

    @Test
    void naoPermiteExcluirEventoComIngressoReservadoOuVendido() {
        Evento comReserva = evento(sessao(AGORA, setor(100, 90, 10, 0)));
        Evento comVenda = evento(sessao(AGORA, setor(100, 90, 0, 10)));

        assertThatThrownBy(comReserva::validarExclusao).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(comVenda::validarExclusao).isInstanceOf(ConflitoException.class);
    }

    @Test
    void permiteExcluirEventoSemMovimentacao() {
        evento(sessao(AGORA, setor(100, 100, 0, 0))).validarExclusao();
    }
}
