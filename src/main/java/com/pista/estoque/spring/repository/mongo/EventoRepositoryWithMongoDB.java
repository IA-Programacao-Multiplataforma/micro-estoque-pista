package com.pista.estoque.spring.repository.mongo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pista.estoque.spring.repository.orm.EventoOrmMongo;

public interface EventoRepositoryWithMongoDB extends MongoRepository<EventoOrmMongo, String> {
    Optional<EventoOrmMongo> findBySessoesId(String sessaoId);
}
