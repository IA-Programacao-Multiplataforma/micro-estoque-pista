package com.pista.estoque.spring.controller.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.StatusSessao;

public record SessaoResponse(
        String id,
        LocalDateTime dataEvento,
        LocalDateTime inicioVenda,
        StatusSessao status,
        List<SetorResponse> setores
) {
}
