package com.pista.estoque.spring.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pista.estoque.domain.evento.PedidoCriado;

// O estoque consome "pedido.criado" (fila estoque.pedido-criado) e publica pedido.realizado / pedido.cancelado
@Configuration
public class Topologia {

    public static final String EXCHANGE = "pista.eventos";
    public static final String EXCHANGE_MORTAS = "pista.eventos.dlx";

    public static final String FILA_PEDIDO_CRIADO = "estoque.pedido-criado";
    public static final String FILA_PEDIDO_CRIADO_DLQ = FILA_PEDIDO_CRIADO + ".dlq";

    @Bean
    public TopicExchange exchangePrincipal() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange exchangeMortas() {
        return new DirectExchange(EXCHANGE_MORTAS, true, false);
    }

    @Bean
    public Queue filaPedidoCriado() {
        return QueueBuilder.durable(FILA_PEDIDO_CRIADO)
                .deadLetterExchange(EXCHANGE_MORTAS)
                .deadLetterRoutingKey(FILA_PEDIDO_CRIADO_DLQ)
                .build();
    }

    // Mensagem que falhou 3 vezes vai para cá (fila morta), para não travar as outras
    @Bean
    public Queue filaPedidoCriadoMorta() {
        return QueueBuilder.durable(FILA_PEDIDO_CRIADO_DLQ).build();
    }

    @Bean
    public Binding ligacaoPedidoCriado() {
        return BindingBuilder.bind(filaPedidoCriado()).to(exchangePrincipal()).with(PedidoCriado.ROUTING_KEY);
    }

    @Bean
    public Binding ligacaoPedidoCriadoMorta() {
        return BindingBuilder.bind(filaPedidoCriadoMorta()).to(exchangeMortas()).with(FILA_PEDIDO_CRIADO_DLQ);
    }
}
