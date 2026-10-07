package com.pista.estoque.spring.messaging;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.pista.estoque.domain.exception.MensagemDuplicadaException;
import com.pista.estoque.domain.service.EventosProcessados;

@Component
public class EventosProcessadosMongo implements EventosProcessados {

    private final MongoTemplate mongoTemplate;

    public EventosProcessadosMongo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean jaProcessado(String eventoId) {
        return mongoTemplate.exists(Query.query(Criteria.where("_id").is(eventoId)), ProcessadoOrmMongo.class);
    }

    @Override
    public void registrar(String eventoId) {
        try {
            mongoTemplate.insert(new ProcessadoOrmMongo(eventoId, Instant.now()));
        } catch (DuplicateKeyException e) {
            throw new MensagemDuplicadaException(eventoId);
        }
    }
}
