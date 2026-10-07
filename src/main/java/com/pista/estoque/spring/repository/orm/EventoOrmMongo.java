package com.pista.estoque.spring.repository.orm;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.pista.estoque.domain.entity.enumerable.Categoria;

@Document(value = "eventos")
public record EventoOrmMongo(
        @Id String id,
        String titulo,
        String descricao,
        Categoria categoria,
        String local,
        String cidade,
        String imagemUrl,
        String imagemAlt,
        List<SessaoOrmMongo> sessoes
) {
}
