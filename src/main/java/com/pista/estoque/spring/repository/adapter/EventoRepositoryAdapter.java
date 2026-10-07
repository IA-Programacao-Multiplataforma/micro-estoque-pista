package com.pista.estoque.spring.repository.adapter;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.spring.repository.orm.EventoOrmMongo;
import com.pista.estoque.spring.repository.orm.SessaoOrmMongo;
import com.pista.estoque.spring.repository.orm.SetorOrmMongo;

public class EventoRepositoryAdapter {
    private EventoRepositoryAdapter() {}

    public static Evento castOrm(EventoOrmMongo orm) {
        return new Evento(
                orm.id(), orm.titulo(), orm.descricao(), orm.categoria(), orm.local(), orm.cidade(),
                orm.imagemUrl(), orm.imagemAlt(),
                orm.sessoes().stream().map(EventoRepositoryAdapter::castSessaoOrm).toList());
    }

    public static EventoOrmMongo castEntity(Evento entity) {
        return new EventoOrmMongo(
                entity.id(), entity.titulo(), entity.descricao(), entity.categoria(), entity.local(), entity.cidade(),
                entity.imagemUrl(), entity.imagemAlt(),
                entity.sessoes().stream().map(EventoRepositoryAdapter::castSessaoEntity).toList());
    }

    private static Sessao castSessaoOrm(SessaoOrmMongo orm) {
        return new Sessao(orm.id(), orm.dataEvento(), orm.inicioVenda(),
                orm.setores().stream().map(EventoRepositoryAdapter::castSetorOrm).toList());
    }

    private static SessaoOrmMongo castSessaoEntity(Sessao entity) {
        return new SessaoOrmMongo(entity.id(), entity.dataEvento(), entity.inicioVenda(),
                entity.setores().stream().map(EventoRepositoryAdapter::castSetorEntity).toList());
    }

    private static Setor castSetorOrm(SetorOrmMongo orm) {
        return new Setor(orm.id(), orm.nome(), orm.preco(), orm.capacidade(),
                orm.disponivel(), orm.reservado(), orm.vendido());
    }

    private static SetorOrmMongo castSetorEntity(Setor entity) {
        return new SetorOrmMongo(entity.id(), entity.nome(), entity.preco(), entity.capacidade(),
                entity.disponivel(), entity.reservado(), entity.vendido());
    }
}
