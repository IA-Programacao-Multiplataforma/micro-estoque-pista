package com.pista.estoque.spring.repository;

import java.time.Instant;
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

    /*
     * LOCK PESSIMISTA no MongoDB (ele não tem "SELECT ... FOR UPDATE"):
     * a primeira operação da transação é uma ESCRITA no documento do evento (campo "travadoEm").
     * O Mongo segura a trava desse documento até o commit ou rollback. Se outra transação tentar
     * travar o mesmo evento nesse meio-tempo, ela recebe "WriteConflict" e espera para tentar de novo
     * (quem repete é o MongoUnidadeDeTrabalho). Resultado: um pedido por vez mexe no estoque do evento.
     */
    @Override
    public boolean travarParaVenda(String sessaoId) {
        Query query = new Query(Criteria.where("sessoes").elemMatch(Criteria.where("id").is(sessaoId)));
        Update trava = new Update().set("travadoEm", Instant.now());
        return mongoTemplate.updateFirst(query, trava, EventoOrmMongo.class).getMatchedCount() == 1;
    }

    // Mesma trava, mas pelo id do evento (usada nas edições do cadastro)
    @Override
    public boolean travarEvento(String eventoId) {
        Update trava = new Update().set("travadoEm", Instant.now());
        return mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(eventoId)), trava, EventoOrmMongo.class)
                .getMatchedCount() == 1;
    }

    @Override
    public void darBaixa(String eventoId, String sessaoId, String setorId, int quantidade) {
        Update update = new Update()
                .inc("sessoes.$[s].setores.$[t].disponivel", -quantidade)
                .inc("sessoes.$[s].setores.$[t].vendido", quantidade)
                .filterArray(Criteria.where("s._id").is(sessaoId))
                .filterArray(Criteria.where("t._id").is(setorId));

        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(eventoId)), update, EventoOrmMongo.class);
    }
}
