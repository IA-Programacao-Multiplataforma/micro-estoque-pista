package com.pista.estoque.spring.controller.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.StatusReserva;

public record ReservaResponse(
        String id,
        String sessaoId,
        String eventoId,
        String usuarioId,
        StatusReserva status,
        Instant expiraEm,
        List<ItemReservaResponse> itens,
        BigDecimal total
) {
}
