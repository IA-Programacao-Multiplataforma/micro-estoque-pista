package com.pista.estoque.spring.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pista.estoque.domain.evento.ReservaConfirmar;

// Cada serviço declara o exchange e as filas que ELE consome; declarar de novo é inofensivo
@Configuration
public class Topologia {

    public static final String EXCHANGE = "pista.eventos";
    public static final String EXCHANGE_MORTAS = "pista.eventos.dlx";

    public static final String FILA_RESERVA_CONFIRMAR = "estoque.reserva-confirmar";
    public static final String FILA_RESERVA_CONFIRMAR_DLQ = FILA_RESERVA_CONFIRMAR + ".dlq";

    @Bean
    public TopicExchange exchangePrincipal() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange exchangeMortas() {
        return new DirectExchange(EXCHANGE_MORTAS, true, false);
    }

    @Bean
    public Queue filaReservaConfirmar() {
        return QueueBuilder.durable(FILA_RESERVA_CONFIRMAR)
                .deadLetterExchange(EXCHANGE_MORTAS)
                .deadLetterRoutingKey(FILA_RESERVA_CONFIRMAR_DLQ)
                .build();
    }

    @Bean
    public Queue filaReservaConfirmarMorta() {
        return QueueBuilder.durable(FILA_RESERVA_CONFIRMAR_DLQ).build();
    }

    @Bean
    public Binding ligacaoReservaConfirmar() {
        return BindingBuilder.bind(filaReservaConfirmar()).to(exchangePrincipal())
                .with(ReservaConfirmar.ROUTING_KEY);
    }

    @Bean
    public Binding ligacaoReservaConfirmarMorta() {
        return BindingBuilder.bind(filaReservaConfirmarMorta()).to(exchangeMortas())
                .with(FILA_RESERVA_CONFIRMAR_DLQ);
    }
}
