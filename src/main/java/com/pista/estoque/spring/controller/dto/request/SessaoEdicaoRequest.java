package com.pista.estoque.spring.controller.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record SessaoEdicaoRequest(
        @NotNull(message = "Data do evento é obrigatória") LocalDateTime dataEvento,
        @NotNull(message = "Início da venda é obrigatório") LocalDateTime inicioVenda
) {
}
