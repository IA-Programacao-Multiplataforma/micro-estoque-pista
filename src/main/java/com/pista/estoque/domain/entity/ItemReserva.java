package com.pista.estoque.domain.entity;

import java.math.BigDecimal;

// Preço congelado na reserva: mudanças posteriores do admin não afetam quem já reservou
public record ItemReserva(
        String setorId,
        String setorNome,
        int quantidade,
        BigDecimal precoUnitario
) {

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
