package com.pista.estoque.spring.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.pista.estoque.domain.entity.enumerable.StatusSessao;

public record DisponibilidadeResponse(
        String sessaoId,
        String eventoId,
        String eventoTitulo,
        LocalDateTime dataEvento,
        LocalDateTime inicioVenda,
        StatusSessao status,
        List<SetorDisponivel> setores
) {

    public record SetorDisponivel(String id, String nome, BigDecimal preco, int disponivel) {
    }
}
