package com.pista.estoque.domain.entity;

import java.math.BigDecimal;

import com.pista.estoque.domain.exception.ConflitoException;
import com.pista.estoque.domain.exception.RegraDeNegocioException;

// Invariante: capacidade = disponivel + reservado + vendido
public record Setor(
        String id,
        String nome,
        BigDecimal preco,
        int capacidade,
        int disponivel,
        int reservado,
        int vendido
) {

    public static Setor novo(String id, String nome, BigDecimal preco, int capacidade) {
        return new Setor(id, nome, preco, capacidade, capacidade, 0, 0);
    }

    public boolean temMovimentacao() {
        return reservado > 0 || vendido > 0;
    }

    public Setor ajustar(Integer novaCapacidade, BigDecimal novoPreco) {
        if (novaCapacidade == null && novoPreco == null) {
            throw new RegraDeNegocioException("Informe a capacidade ou o preço");
        }

        int capacidadeFinal = capacidade;
        BigDecimal precoFinal = preco;

        if (novaCapacidade != null) {
            if (novaCapacidade <= 0) {
                throw new RegraDeNegocioException("Capacidade deve ser maior que zero");
            }
            if (novaCapacidade < reservado + vendido) {
                throw new ConflitoException(
                        "Não é possível reduzir a capacidade abaixo do que já foi reservado ou vendido");
            }
            capacidadeFinal = novaCapacidade;
        }

        if (novoPreco != null) {
            if (novoPreco.signum() < 0) {
                throw new RegraDeNegocioException("Preço não pode ser negativo");
            }
            precoFinal = novoPreco;
        }

        return new Setor(id, nome, precoFinal, capacidadeFinal,
                capacidadeFinal - reservado - vendido, reservado, vendido);
    }
}
