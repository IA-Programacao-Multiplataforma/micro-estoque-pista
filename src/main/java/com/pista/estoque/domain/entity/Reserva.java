package com.pista.estoque.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.StatusReserva;

// usuarioId é o e-mail do usuário
public record Reserva(
        String id,
        String sessaoId,
        String eventoId,
        String usuarioId,
        StatusReserva status,
        Instant expiraEm,
        Instant criadaEm,
        List<ItemReserva> itens
) {

    public Reserva {
        itens = List.copyOf(itens);
    }

    public BigDecimal total() {
        return itens.stream().map(ItemReserva::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
