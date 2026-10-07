package com.pista.estoque.spring.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.domain.repository.ReservaRepository;
import com.pista.estoque.domain.service.EventosProcessados;
import com.pista.estoque.domain.service.PublicadorDeEventos;
import com.pista.estoque.domain.service.ReservaService;
import com.pista.estoque.domain.service.UnidadeDeTrabalho;

@Configuration
@EnableScheduling
public class ReservaConfig {

    // Fuso da máquina: as datas dos eventos são "hora local" (sem fuso)
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public UnidadeDeTrabalho unidadeDeTrabalho(MongoTransactionManager transactionManager) {
        return new MongoUnidadeDeTrabalho(transactionManager);
    }

    @Bean
    public ReservaService reservaService(
            EventoRepository eventos,
            ReservaRepository reservas,
            @Value("${estoque.reserva.max-ingressos-por-compra}") int maxIngressosPorCompra,
            Clock clock,
            UnidadeDeTrabalho unidade,
            PublicadorDeEventos publicador,
            EventosProcessados processados) {
        return new ReservaService(eventos, reservas, maxIngressosPorCompra, clock, unidade, publicador, processados);
    }
}
