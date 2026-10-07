package com.pista.estoque.spring.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pista.estoque.domain.evento.ReservaConfirmar;
import com.pista.estoque.domain.service.ReservaService;

// Se lançar exceção, o Spring tenta de novo (3x) e depois manda para a DLQ
@Component
public class ReservaConfirmarListener {

    private static final Logger log = LoggerFactory.getLogger(ReservaConfirmarListener.class);

    private final MensagemJson mensagens;
    private final ReservaService service;

    public ReservaConfirmarListener(MensagemJson mensagens, ReservaService service) {
        this.mensagens = mensagens;
        this.service = service;
    }

    @RabbitListener(queues = Topologia.FILA_RESERVA_CONFIRMAR)
    public void receber(String corpo) {
        MensagemJson.Recebida mensagem = mensagens.ler(corpo);
        ReservaConfirmar comando = mensagens.dados(mensagem, ReservaConfirmar.class);
        log.info("[saga] recebi reserva.confirmar: pedido={} reserva={} eventoId={}",
                comando.pedidoId(), comando.reservaId(), mensagem.eventoId());

        service.processarConfirmacao(mensagem.eventoId(), comando);
    }
}
