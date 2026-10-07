package com.pista.estoque.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.StatusSessao;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;

public record Sessao(
        String id,
        LocalDateTime dataEvento,
        LocalDateTime inicioVenda,
        List<Setor> setores
) {

    public Sessao {
        if (inicioVenda.isAfter(dataEvento)) {
            throw new RegraDeNegocioException("O início da venda não pode ser depois da data do evento");
        }
        setores = List.copyOf(setores);
    }

    public StatusSessao status(LocalDateTime agora) {
        if (agora.isBefore(inicioVenda)) {
            return StatusSessao.ESTREIA;
        }
        int disponivel = setores.stream().mapToInt(Setor::disponivel).sum();
        return disponivel > 0 ? StatusSessao.DISPONIVEL : StatusSessao.ESGOTADO;
    }

    public boolean temMovimentacao() {
        return setores.stream().anyMatch(Setor::temMovimentacao);
    }

    public Sessao comSetorAjustado(String setorId, Integer capacidade, BigDecimal preco) {
        boolean achou = setores.stream().anyMatch(s -> s.id().equals(setorId));
        if (!achou) {
            throw new NaoEncontradoException("Setor não encontrado");
        }

        List<Setor> novos = setores.stream()
                .map(s -> s.id().equals(setorId) ? s.ajustar(capacidade, preco) : s)
                .toList();
        return new Sessao(id, dataEvento, inicioVenda, novos);
    }
}
