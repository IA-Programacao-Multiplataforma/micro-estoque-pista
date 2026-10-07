package com.pista.estoque.spring.controller.dto.response;

import java.util.List;

import com.pista.estoque.domain.entity.enumerable.Categoria;

public record EventoResponse(
        String id,
        String titulo,
        String descricao,
        Categoria categoria,
        String local,
        String cidade,
        String imagemUrl,
        String imagemAlt,
        List<SessaoResponse> sessoes
) {
}
