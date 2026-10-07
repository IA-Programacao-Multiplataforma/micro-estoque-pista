package com.pista.estoque.spring.controller.dto.response;

import java.math.BigDecimal;

public record ItemReservaResponse(
        String setorId,
        String setorNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}
