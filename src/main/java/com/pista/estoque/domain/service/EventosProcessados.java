package com.pista.estoque.domain.service;

// Idempotência: o RabbitMQ pode entregar a mesma mensagem mais de uma vez, então o consumidor confere o eventoId aqui
public interface EventosProcessados {

    boolean jaProcessado(String eventoId);

    // Dentro da transação do efeito: eventoId repetido lança MensagemDuplicadaException e a transação inteira é desfeita
    void registrar(String eventoId);
}
