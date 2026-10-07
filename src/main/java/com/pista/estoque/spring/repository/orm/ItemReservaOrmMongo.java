package com.pista.estoque.spring.repository.orm;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public record ItemReservaOrmMongo(
        String setorId,
        String setorNome,
        int quantidade,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal precoUnitario
) {
}
