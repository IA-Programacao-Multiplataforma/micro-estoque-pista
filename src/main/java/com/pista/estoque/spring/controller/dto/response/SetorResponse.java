package com.pista.estoque.spring.controller.dto.response;

import java.math.BigDecimal;

public record SetorResponse(
        String id,
        String nome,
        BigDecimal preco,
        int capacidade,
        int disponivel,
        int reservado,
        int vendido
) {
}
