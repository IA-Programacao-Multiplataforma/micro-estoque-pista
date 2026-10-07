package com.pista.estoque.spring.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.pista.estoque.domain.entity.Reserva;
import com.pista.estoque.domain.entity.enumerable.StatusReserva;
import com.pista.estoque.domain.repository.ReservaRepository;
import com.pista.estoque.spring.repository.adapter.ReservaRepositoryAdapter;
import com.pista.estoque.spring.repository.mongo.ReservaRepositoryWithMongoDB;
import com.pista.estoque.spring.repository.orm.ReservaOrmMongo;

@Repository
public class ReservaRepositoryImpl implements ReservaRepository {

    private final ReservaRepositoryWithMongoDB repository;
    private final MongoTemplate mongoTemplate;

    public ReservaRepositoryImpl(ReservaRepositoryWithMongoDB repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Reserva salvar(Reserva reserva) {
        return ReservaRepositoryAdapter.castOrm(repository.save(ReservaRepositoryAdapter.castEntity(reserva)));
    }

    @Override
    public Optional<Reserva> buscarPorId(String id) {
        return repository.findById(id).map(ReservaRepositoryAdapter::castOrm);
    }

    @Override
    public Optional<Reserva> buscarAtivaPorSessaoEUsuario(String sessaoId, String usuarioId) {
        return repository.findFirstBySessaoIdAndUsuarioIdAndStatus(sessaoId, usuarioId, StatusReserva.ATIVA)
                .map(ReservaRepositoryAdapter::castOrm);
    }

    @Override
    public List<Reserva> listarAtivasVencidasAte(Instant limite) {
        return repository.findByStatusAndExpiraEmBefore(StatusReserva.ATIVA, limite).stream()
                .map(ReservaRepositoryAdapter::castOrm)
                .toList();
    }

    // Confere e troca na mesma operação: só uma de duas chamadas simultâneas consegue (modifiedCount == 1)
    @Override
    public boolean transicionar(String id, StatusReserva de, StatusReserva para) {
        Query query = new Query(Criteria.where("_id").is(id).and("status").is(de));
        Update update = new Update().set("status", para);
        return mongoTemplate.updateFirst(query, update, ReservaOrmMongo.class).getModifiedCount() == 1;
    }
}
