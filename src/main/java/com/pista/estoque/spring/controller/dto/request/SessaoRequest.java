package com.pista.estoque.spring.controller.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SessaoRequest(
        @NotNull(message = "Data do evento é obrigatória") LocalDateTime dataEvento,
        @NotNull(message = "Início da venda é obrigatório") LocalDateTime inicioVenda,
        @NotEmpty(message = "Informe pelo menos um setor") @Valid List<SetorRequest> setores
) {
}
