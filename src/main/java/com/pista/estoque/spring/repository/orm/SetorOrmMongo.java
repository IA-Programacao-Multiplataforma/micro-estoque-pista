package com.pista.estoque.spring.repository.orm;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public record SetorOrmMongo(
        String id,
        String nome,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal preco,
        int capacidade,
        int disponivel,
        int reservado,
        int vendido
) {
}
