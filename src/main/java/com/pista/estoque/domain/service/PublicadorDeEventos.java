package com.pista.estoque.domain.service;

// Envia uma mensagem ao RabbitMQ (exchange pista.eventos) com a routing key informada
public interface PublicadorDeEventos {
    void publicar(String routingKey, Object dados);
}
