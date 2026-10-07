package com.pista.estoque.domain.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.NaoEncontradoException;

public record Evento(
        String id,
        String titulo,
        String descricao,
        Categoria categoria,
        String local,
        String cidade,
        String imagemUrl,
        String imagemAlt,
        List<Sessao> sessoes
) {

    public Evento {
        sessoes = List.copyOf(sessoes);
    }

    public Evento comNovaSessao(Sessao sessao) {
        List<Sessao> novas = new ArrayList<>(sessoes);
        novas.add(sessao);
        return comSessoes(novas);
    }

    public Evento comSetorAjustado(String sessaoId, String setorId, Integer capacidade, BigDecimal preco) {
        boolean achou = sessoes.stream().anyMatch(s -> s.id().equals(sessaoId));
        if (!achou) {
            throw new NaoEncontradoException("Sessão não encontrada");
        }

        List<Sessao> novas = sessoes.stream()
                .map(s -> s.id().equals(sessaoId) ? s.comSetorAjustado(setorId, capacidade, preco) : s)
                .toList();
        return comSessoes(novas);
    }

    public void validarExclusao() {
        if (sessoes.stream().anyMatch(Sessao::temMovimentacao)) {
            throw new ConflitoException("Evento com ingressos reservados ou vendidos não pode ser excluído");
        }
    }

    private Evento comSessoes(List<Sessao> novas) {
        return new Evento(id, titulo, descricao, categoria, local, cidade, imagemUrl, imagemAlt, novas);
    }
}
