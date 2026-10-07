package com.pista.estoque.domain.evento;

public record ReservaConfirmada(String pedidoId, String reservaId) {

    public static final String ROUTING_KEY = "reserva.confirmada";
}
