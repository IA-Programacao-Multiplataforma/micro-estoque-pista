package com.pista.estoque.domain.evento;

import java.util.List;

// Chega do Pedido: "este cliente quer estes ingressos desta sessão"
public record PedidoCriado(String pedidoId, String sessaoId, List<Item> itens) {

    public static final String ROUTING_KEY = "pedido.criado";

    public record Item(String setorId, int quantidade) {
    }
}
