package com.pista.estoque.domain.entity;

import java.math.BigDecimal;

import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;

// Invariante: capacidade = disponivel + vendido
public record Setor(
        String id,
        String nome,
        BigDecimal preco,
        int capacidade,
        int disponivel,
        int vendido
) {

    public static Setor novo(String id, String nome, BigDecimal preco, int capacidade) {
        return new Setor(id, nome, preco, capacidade, capacidade, 0);
    }

    public boolean temMovimentacao() {
        return vendido > 0;
    }

    public Setor ajustar(Integer novaCapacidade, BigDecimal novoPreco) {
        return ajustar(null, novaCapacidade, novoPreco);
    }

    // Só o que foi informado muda. Reduzir a capacidade abaixo do vendido não é permitido.
    public Setor ajustar(String novoNome, Integer novaCapacidade, BigDecimal novoPreco) {
        if (novoNome == null && novaCapacidade == null && novoPreco == null) {
            throw new RegraDeNegocioException("Informe o nome, a capacidade ou o preço");
        }

        String nomeFinal = nome;
        int capacidadeFinal = capacidade;
        BigDecimal precoFinal = preco;

        if (novoNome != null) {
            if (novoNome.isBlank()) {
                throw new RegraDeNegocioException("O nome do setor não pode ficar em branco");
            }
            nomeFinal = novoNome.trim();
        }
        if (novaCapacidade != null) {
            if (novaCapacidade <= 0) {
                throw new RegraDeNegocioException("Capacidade deve ser maior que zero");
            }
            if (novaCapacidade < vendido) {
                throw new ConflitoException("Não é possível reduzir a capacidade abaixo do que já foi vendido");
            }
            capacidadeFinal = novaCapacidade;
        }
        if (novoPreco != null) {
            if (novoPreco.signum() < 0) {
                throw new RegraDeNegocioException("Preço não pode ser negativo");
            }
            precoFinal = novoPreco;
        }

        return new Setor(id, nomeFinal, precoFinal, capacidadeFinal, capacidadeFinal - vendido, vendido);
    }
}
