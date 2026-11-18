package com.pista.estoque.spring.controller.dto.request;

import java.math.BigDecimal;

// Só o que for informado muda
public record SetorAjusteRequest(
        String nome,
        Integer capacidade,
        BigDecimal preco
) {
}
