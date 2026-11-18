package com.pista.estoque.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;

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

    // ---------------------------------------------------------------- dados do evento

    public Evento comDadosEditados(String novoTitulo, String novaDescricao, Categoria novaCategoria,
                                   String novoLocal, String novaCidade, String novaImagemUrl, String novaImagemAlt) {
        if (novoTitulo == null || novoTitulo.isBlank()) {
            throw new RegraDeNegocioException("Título é obrigatório");
        }
        if (novaCategoria == null) {
            throw new RegraDeNegocioException("Categoria é obrigatória");
        }
        if (novoLocal == null || novoLocal.isBlank()) {
            throw new RegraDeNegocioException("Local é obrigatório");
        }
        if (novaCidade == null || novaCidade.isBlank()) {
            throw new RegraDeNegocioException("Cidade é obrigatória");
        }
        return new Evento(id, novoTitulo.trim(), novaDescricao, novaCategoria, novoLocal.trim(), novaCidade.trim(),
                novaImagemUrl, novaImagemAlt, sessoes);
    }

    // ---------------------------------------------------------------- datas (sessões)

    public Evento comNovaSessao(Sessao sessao) {
        List<Sessao> novas = new ArrayList<>(sessoes);
        novas.add(sessao);
        return comSessoes(novas);
    }

    public Evento comSessaoEditada(String sessaoId, LocalDateTime dataEvento, LocalDateTime inicioVenda) {
        return trocarSessao(sessaoId, s -> s.comDatas(dataEvento, inicioVenda));
    }

    // Uma data com ingressos vendidos não some, e o evento precisa continuar com pelo menos uma data
    public Evento semSessao(String sessaoId) {
        Sessao sessao = buscarSessao(sessaoId);
        if (sessao.temMovimentacao()) {
            throw new ConflitoException("Data com ingressos vendidos não pode ser excluída");
        }
        if (sessoes.size() == 1) {
            throw new RegraDeNegocioException("O evento precisa ter pelo menos uma data");
        }
        return comSessoes(sessoes.stream().filter(s -> !s.id().equals(sessaoId)).toList());
    }

    // ---------------------------------------------------------------- setores (tipos de ingresso)

    public Evento comSetorNaSessao(String sessaoId, Setor novo) {
        return trocarSessao(sessaoId, s -> s.comNovoSetor(novo));
    }

    public Evento comSetorAjustado(String sessaoId, String setorId, Integer capacidade, BigDecimal preco) {
        return comSetorAjustado(sessaoId, setorId, null, capacidade, preco);
    }

    public Evento comSetorAjustado(String sessaoId, String setorId, String nome, Integer capacidade, BigDecimal preco) {
        return trocarSessao(sessaoId, s -> s.comSetorAjustado(setorId, nome, capacidade, preco));
    }

    public Evento semSetor(String sessaoId, String setorId) {
        return trocarSessao(sessaoId, s -> s.semSetor(setorId));
    }

    // ---------------------------------------------------------------- exclusão

    public void validarExclusao() {
        if (sessoes.stream().anyMatch(Sessao::temMovimentacao)) {
            throw new ConflitoException("Evento com ingressos vendidos não pode ser excluído");
        }
    }

    // ---------------------------------------------------------------- apoio

    private Sessao buscarSessao(String sessaoId) {
        return sessoes.stream()
                .filter(s -> s.id().equals(sessaoId))
                .findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Sessão não encontrada"));
    }

    private Evento trocarSessao(String sessaoId, UnaryOperator<Sessao> mudanca) {
        buscarSessao(sessaoId);
        return comSessoes(sessoes.stream().map(s -> s.id().equals(sessaoId) ? mudanca.apply(s) : s).toList());
    }

    private Evento comSessoes(List<Sessao> novas) {
        return new Evento(id, titulo, descricao, categoria, local, cidade, imagemUrl, imagemAlt, novas);
    }
}
