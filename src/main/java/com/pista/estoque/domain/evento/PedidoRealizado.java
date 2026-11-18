package com.pista.estoque.domain.evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Vai para o Pedido: havia ingressos e a baixa no estoque foi feita
public record PedidoRealizado(
        String pedidoId,
        String eventoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        List<Item> itens
) {

    public static final String ROUTING_KEY = "pedido.realizado";

    public record Item(String setorId, String setorNome, int quantidade, BigDecimal precoUnitario) {
    }
}
