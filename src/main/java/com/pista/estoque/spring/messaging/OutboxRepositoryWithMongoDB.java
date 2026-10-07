package com.pista.estoque.spring.messaging;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutboxRepositoryWithMongoDB extends MongoRepository<OutboxOrmMongo, String> {

    List<OutboxOrmMongo> findByPublicadoEmIsNullOrderByCriadoEmAsc(Pageable pageable);
}
