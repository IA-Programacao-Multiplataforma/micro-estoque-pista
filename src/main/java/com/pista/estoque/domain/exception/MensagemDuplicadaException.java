package com.pista.estoque.domain.exception;

public class MensagemDuplicadaException extends RuntimeException {

    public MensagemDuplicadaException(String eventoId) {
        super("Mensagem já processada: " + eventoId);
    }
}
