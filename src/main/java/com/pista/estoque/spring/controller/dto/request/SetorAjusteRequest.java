package com.pista.estoque.spring.controller.dto.request;

import java.math.BigDecimal;

public record SetorAjusteRequest(
        Integer capacidade,
        BigDecimal preco
) {
}
