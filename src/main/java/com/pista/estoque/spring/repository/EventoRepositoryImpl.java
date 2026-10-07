package com.pista.estoque.spring.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.spring.repository.adapter.EventoRepositoryAdapter;
import com.pista.estoque.spring.repository.mongo.EventoRepositoryWithMongoDB;
import com.pista.estoque.spring.repository.orm.EventoOrmMongo;

@Repository
public class EventoRepositoryImpl implements EventoRepository {

    private final EventoRepositoryWithMongoDB repository;
    private final MongoTemplate mongoTemplate;

    public EventoRepositoryImpl(EventoRepositoryWithMongoDB repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Evento salvar(Evento evento) {
        EventoOrmMongo orm = EventoRepositoryAdapter.castEntity(evento);
        return EventoRepositoryAdapter.castOrm(repository.save(orm));
    }

    @Override
    public void deletar(String id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Evento> buscarPorId(String id) {
        return repository.findById(id).map(EventoRepositoryAdapter::castOrm);
    }

    @Override
    public Optional<Evento> buscarPorSessaoId(String sessaoId) {
        return repository.findBySessoesId(sessaoId).map(EventoRepositoryAdapter::castOrm);
    }

    @Override
    public List<Evento> listar() {
        return repository.findAll().stream().map(EventoRepositoryAdapter::castOrm).toList();
    }

    // Filtro "disponivel >= quantidade" e $inc na mesma operação: dois clientes nunca levam o mesmo último ingresso
    @Override
    public boolean reservarIngressos(String eventoId, String sessaoId, String setorId, int quantidade) {
        Query query = new Query(Criteria.where("_id").is(eventoId)
                .and("sessoes").elemMatch(Criteria.where("id").is(sessaoId)
                        .and("setores").elemMatch(Criteria.where("id").is(setorId)
                                .and("disponivel").gte(quantidade))));

        return mongoTemplate.updateFirst(query, ajusteDeEstoque(sessaoId, setorId, -quantidade, quantidade),
                EventoOrmMongo.class).getModifiedCount() == 1;
    }

    @Override
    public void devolverIngressos(String eventoId, String sessaoId, String setorId, int quantidade) {
        Query query = new Query(Criteria.where("_id").is(eventoId)
                .and("sessoes").elemMatch(Criteria.where("id").is(sessaoId)
                        .and("setores").elemMatch(Criteria.where("id").is(setorId)
                                .and("reservado").gte(quantidade))));

        mongoTemplate.updateFirst(query, ajusteDeEstoque(sessaoId, setorId, quantidade, -quantidade),
                EventoOrmMongo.class);
    }

    @Override
    public void confirmarVenda(String eventoId, String sessaoId, String setorId, int quantidade) {
        Query query = new Query(Criteria.where("_id").is(eventoId)
                .and("sessoes").elemMatch(Criteria.where("id").is(sessaoId)
                        .and("setores").elemMatch(Criteria.where("id").is(setorId)
                                .and("reservado").gte(quantidade))));

        Update update = new Update()
                .inc("sessoes.$[s].setores.$[t].reservado", -quantidade)
                .inc("sessoes.$[s].setores.$[t].vendido", quantidade)
                .filterArray(Criteria.where("s._id").is(sessaoId))
                .filterArray(Criteria.where("t._id").is(setorId));

        mongoTemplate.updateFirst(query, update, EventoOrmMongo.class);
    }

    private Update ajusteDeEstoque(String sessaoId, String setorId, int deltaDisponivel, int deltaReservado) {
        return new Update()
                .inc("sessoes.$[s].setores.$[t].disponivel", deltaDisponivel)
                .inc("sessoes.$[s].setores.$[t].reservado", deltaReservado)
                .filterArray(Criteria.where("s._id").is(sessaoId))
                .filterArray(Criteria.where("t._id").is(setorId));
    }
}
