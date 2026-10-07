package com.pista.estoque.spring.messaging;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// O _id único garante a idempotência: inserir o mesmo eventoId duas vezes falha
@Document(value = "eventos_processados")
public record ProcessadoOrmMongo(@Id String eventoId, Instant processadoEm) {
}
