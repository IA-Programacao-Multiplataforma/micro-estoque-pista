package com.pista.estoque.domain.evento;

// Vai para o Pedido: não havia ingressos (ou o pedido era inválido); nada foi baixado
public record PedidoCancelado(String pedidoId, String motivo) {

    public static final String ROUTING_KEY = "pedido.cancelado";
}
