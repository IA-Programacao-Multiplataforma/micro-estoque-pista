package com.pista.estoque.spring.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pista.estoque.domain.service.ReservaService;

@Component
public class ReservaExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaExpiracaoScheduler.class);

    private final ReservaService service;

    public ReservaExpiracaoScheduler(ReservaService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${estoque.reserva.intervalo-expiracao-ms}")
    public void expirarReservasVencidas() {
        try {
            int expiradas = service.expirarVencidas();
            if (expiradas > 0) {
                log.info("{} reserva(s) expirada(s); ingressos devolvidos ao estoque", expiradas);
            }
        } catch (RuntimeException e) {
            // Uma falha (ex.: banco fora do ar) não pode matar a tarefa: tenta de novo no próximo ciclo
            log.warn("Falha ao expirar reservas: {}", e.getMessage());
        }
    }
}
