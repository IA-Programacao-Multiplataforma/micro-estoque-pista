package com.pista.estoque.spring.messaging;

import java.time.Instant;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.pista.estoque.domain.service.PublicadorDeEventos;

@Component
public class OutboxPublicador implements PublicadorDeEventos {

    private final MongoTemplate mongoTemplate;
    private final MensagemJson mensagens;

    public OutboxPublicador(MongoTemplate mongoTemplate, MensagemJson mensagens) {
        this.mongoTemplate = mongoTemplate;
        this.mensagens = mensagens;
    }

    @Override
    public void publicar(String routingKey, Object dados) {
        MensagemJson.Pronta pronta = mensagens.montar(routingKey, dados);
        mongoTemplate.insert(new OutboxOrmMongo(pronta.eventoId(), routingKey, pronta.json(),
                Instant.now(), null, 0));
    }
}
