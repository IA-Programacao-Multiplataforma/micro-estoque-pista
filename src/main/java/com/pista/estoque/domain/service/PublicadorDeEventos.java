package com.pista.estoque.domain.service;

// Outbox: a mensagem é gravada na mesma transação do efeito (os dois ou nenhum) e publicada no RabbitMQ depois
public interface PublicadorDeEventos {
    void publicar(String routingKey, Object dados);
}
