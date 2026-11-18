package com.pista.estoque.spring.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pista.estoque.domain.evento.PedidoCriado;
import com.pista.estoque.domain.service.VendaService;

// Só recebe a mensagem e entrega ao serviço; a regra de negócio fica no VendaService
@Component
public class PedidoCriadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoCriadoListener.class);

    private final MensagemJson mensagens;
    private final VendaService service;

    public PedidoCriadoListener(MensagemJson mensagens, VendaService service) {
        this.mensagens = mensagens;
        this.service = service;
    }

    @RabbitListener(queues = Topologia.FILA_PEDIDO_CRIADO)
    public void receber(String corpo) {
        PedidoCriado pedido = mensagens.dados(mensagens.ler(corpo), PedidoCriado.class);
        log.info("[estoque] recebi pedido.criado: pedido={} sessao={}", pedido.pedidoId(), pedido.sessaoId());
        service.processar(pedido);
    }
}
