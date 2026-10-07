package com.pista.estoque.spring.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Só marca "publicado" depois que o broker confirma; se o RabbitMQ cair, a mensagem espera na outbox.
// Pode publicar a mesma mensagem duas vezes, por isso os consumidores são idempotentes.
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int LOTE = 50;
    private static final long ESPERA_CONFIRMACAO_MS = 5000;

    private final OutboxRepositoryWithMongoDB outbox;
    private final RabbitTemplate rabbit;
    private final MongoTemplate mongoTemplate;

    public OutboxPublisher(OutboxRepositoryWithMongoDB outbox, RabbitTemplate rabbit, MongoTemplate mongoTemplate) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.mongoTemplate = mongoTemplate;
    }

    @Scheduled(fixedDelayString = "${mensageria.outbox.intervalo-ms:1000}")
    public void publicarPendentes() {
        List<OutboxOrmMongo> pendentes;
        try {
            pendentes = outbox.findByPublicadoEmIsNullOrderByCriadoEmAsc(PageRequest.of(0, LOTE));
        } catch (RuntimeException e) {
            log.warn("Não foi possível ler a outbox: {}", e.getMessage());
            return;
        }

        for (OutboxOrmMongo item : pendentes) {
            try {
                publicar(item);
                marcar(item.id(), Update.update("publicadoEm", Instant.now()));
            } catch (RuntimeException e) {
                // Para no primeiro erro, para não furar a ordem; tenta de novo no próximo ciclo
                marcar(item.id(), new Update().inc("tentativas", 1));
                log.warn("Mensagem {} ({}) não publicada, ficará na outbox: {}",
                        item.id(), item.routingKey(), e.getMessage());
                return;
            }
        }
    }

    private void publicar(OutboxOrmMongo item) {
        Message mensagem = MessageBuilder.withBody(item.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setContentEncoding("UTF-8")
                .setMessageId(item.id())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbit.invoke(operacoes -> {
            operacoes.send(Topologia.EXCHANGE, item.routingKey(), mensagem);
            operacoes.waitForConfirmsOrDie(ESPERA_CONFIRMACAO_MS);
            return null;
        });
    }

    private void marcar(String id, Update update) {
        try {
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)), update, OutboxOrmMongo.class);
        } catch (RuntimeException e) {
            log.warn("Falha ao atualizar a outbox ({}): {}", id, e.getMessage());
        }
    }
}
