package com.pista.estoque.spring.messaging;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value = "outbox")
public record OutboxOrmMongo(
        @Id String id,
        String routingKey,
        String payload,
        Instant criadoEm,
        Instant publicadoEm,
        int tentativas
) {
}
