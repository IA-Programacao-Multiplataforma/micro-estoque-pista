package com.pista.estoque.spring.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.domain.service.EventoService;
import com.pista.estoque.domain.service.PublicadorDeEventos;
import com.pista.estoque.domain.service.UnidadeDeTrabalho;
import com.pista.estoque.domain.service.VendaService;

@Configuration
public class EstoqueConfig {

    // Fuso da máquina: as datas dos eventos são "hora local" (sem fuso)
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    // Habilita transações no MongoDB
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public UnidadeDeTrabalho unidadeDeTrabalho(MongoTransactionManager transactionManager) {
        return new MongoUnidadeDeTrabalho(transactionManager);
    }

    @Bean
    public EventoService eventoService(EventoRepository eventos, UnidadeDeTrabalho transacao) {
        return new EventoService(eventos, transacao);
    }

    @Bean
    public VendaService vendaService(
            EventoRepository eventos,
            UnidadeDeTrabalho transacao,
            PublicadorDeEventos publicador,
            @Value("${estoque.max-ingressos-por-compra}") int maxIngressosPorCompra,
            Clock clock) {
        return new VendaService(eventos, transacao, publicador, maxIngressosPorCompra, clock);
    }
}
