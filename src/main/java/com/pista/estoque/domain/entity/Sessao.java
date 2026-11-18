package com.pista.estoque.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.StatusSessao;
import com.pista.estoque.domain.exception.ConflitoException;
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

    // Troca a data do evento e o início da venda (o construtor confere a ordem entre as duas)
    public Sessao comDatas(LocalDateTime novaData, LocalDateTime novoInicio) {
        if (novaData == null || novoInicio == null) {
            throw new RegraDeNegocioException("Informe a data do evento e o início da venda");
        }
        return new Sessao(id, novaData, novoInicio, setores);
    }

    public Sessao comNovoSetor(Setor novo) {
        garantirNomeLivre(novo.nome(), null);
        List<Setor> novos = new ArrayList<>(setores);
        novos.add(novo);
        return new Sessao(id, dataEvento, inicioVenda, novos);
    }

    public Sessao comSetorAjustado(String setorId, Integer capacidade, BigDecimal preco) {
        return comSetorAjustado(setorId, null, capacidade, preco);
    }

    public Sessao comSetorAjustado(String setorId, String nome, Integer capacidade, BigDecimal preco) {
        buscarSetor(setorId);
        if (nome != null && !nome.isBlank()) {
            garantirNomeLivre(nome, setorId);
        }
        List<Setor> novos = setores.stream()
                .map(s -> s.id().equals(setorId) ? s.ajustar(nome, capacidade, preco) : s)
                .toList();
        return new Sessao(id, dataEvento, inicioVenda, novos);
    }

    // Um setor com ingressos vendidos não pode sumir, e a data precisa continuar com pelo menos um setor
    public Sessao semSetor(String setorId) {
        Setor setor = buscarSetor(setorId);
        if (setor.temMovimentacao()) {
            throw new ConflitoException("Setor com ingressos vendidos não pode ser excluído");
        }
        if (setores.size() == 1) {
            throw new RegraDeNegocioException("A data precisa ter pelo menos um setor");
        }
        return new Sessao(id, dataEvento, inicioVenda, setores.stream().filter(s -> !s.id().equals(setorId)).toList());
    }

    private Setor buscarSetor(String setorId) {
        return setores.stream()
                .filter(s -> s.id().equals(setorId))
                .findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Setor não encontrado"));
    }

    // Dois setores com o mesmo nome na mesma data confundiriam quem compra
    private void garantirNomeLivre(String nome, String exceto) {
        boolean repetido = setores.stream()
                .filter(s -> !s.id().equals(exceto))
                .anyMatch(s -> s.nome().trim().equalsIgnoreCase(nome.trim()));
        if (repetido) {
            throw new ConflitoException("Já existe um setor chamado \"" + nome.trim() + "\" nesta data");
        }
    }
}
