package com.pista.estoque.domain.exception;

// Lançada dentro da transação: desfaz tudo (rollback) e vira a mensagem pedido.cancelado
public class VendaRecusadaException extends RuntimeException {

    public VendaRecusadaException(String motivo) {
        super(motivo);
    }
}
