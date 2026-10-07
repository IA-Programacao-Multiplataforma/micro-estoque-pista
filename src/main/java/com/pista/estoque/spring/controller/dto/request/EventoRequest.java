package com.pista.estoque.spring.controller.dto.request;

import java.util.List;

import com.pista.estoque.domain.entity.enumerable.Categoria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record EventoRequest(
        @NotBlank(message = "Título é obrigatório") String titulo,
        String descricao,
        @NotNull(message = "Categoria é obrigatória") Categoria categoria,
        @NotBlank(message = "Local é obrigatório") String local,
        @NotBlank(message = "Cidade é obrigatória") String cidade,
        String imagemUrl,
        String imagemAlt,
        @NotEmpty(message = "Informe pelo menos uma sessão") @Valid List<SessaoRequest> sessoes
) {
}
