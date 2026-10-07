package com.pista.estoque.domain.exception;

public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String message) {
        super(message);
    }
}
