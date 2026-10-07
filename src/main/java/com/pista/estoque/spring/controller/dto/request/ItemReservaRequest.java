package com.pista.estoque.spring.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemReservaRequest(
        @NotBlank(message = "Setor é obrigatório") String setorId,
        @NotNull(message = "Quantidade é obrigatória") Integer quantidade
) {
}
