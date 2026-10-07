package com.pista.estoque.spring.repository.orm;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.pista.estoque.domain.entity.enumerable.StatusReserva;

@Document(value = "reservas")
public record ReservaOrmMongo(
        @Id String id,
        String sessaoId,
        String eventoId,
        String usuarioId,
        StatusReserva status,
        Instant expiraEm,
        Instant criadaEm,
        List<ItemReservaOrmMongo> itens
) {
}
