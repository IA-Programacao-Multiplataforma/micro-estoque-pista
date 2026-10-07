package com.pista.estoque.domain.evento;

public record ReservaConfirmar(String pedidoId, String reservaId) {

    public static final String ROUTING_KEY = "reserva.confirmar";
}
