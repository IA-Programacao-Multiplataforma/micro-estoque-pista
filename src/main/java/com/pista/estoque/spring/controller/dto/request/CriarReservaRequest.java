package com.pista.estoque.spring.controller.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CriarReservaRequest(
        @NotBlank(message = "Sessão é obrigatória") String sessaoId,
        @NotBlank(message = "Usuário é obrigatório") String usuarioId,
        @NotNull(message = "Data de expiração é obrigatória") Instant expiraEm,
        @NotEmpty(message = "Informe pelo menos um item") @Valid List<ItemReservaRequest> itens
) {
}
