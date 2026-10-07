package com.pista.estoque.spring.controller.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SetorRequest(
        @NotBlank(message = "Nome do setor é obrigatório") String nome,
        @NotNull(message = "Preço é obrigatório")
        @PositiveOrZero(message = "Preço não pode ser negativo") BigDecimal preco,
        @NotNull(message = "Capacidade é obrigatória")
        @Positive(message = "Capacidade deve ser maior que zero") Integer capacidade
) {
}
