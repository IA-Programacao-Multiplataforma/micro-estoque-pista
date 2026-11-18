package com.pista.estoque.spring.controller.dto.request;

import com.pista.estoque.domain.entity.enumerable.Categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Dados do evento que o administrador pode editar (as datas e os setores têm rotas próprias)
public record EventoEdicaoRequest(
        @NotBlank(message = "Título é obrigatório") String titulo,
        String descricao,
        @NotNull(message = "Categoria é obrigatória") Categoria categoria,
        @NotBlank(message = "Local é obrigatório") String local,
        @NotBlank(message = "Cidade é obrigatória") String cidade,
        String imagemUrl,
        String imagemAlt
) {
}
