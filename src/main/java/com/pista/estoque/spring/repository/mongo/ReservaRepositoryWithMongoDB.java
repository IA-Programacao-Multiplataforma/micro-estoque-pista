package com.pista.estoque.spring.repository.mongo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pista.estoque.domain.entity.enumerable.StatusReserva;
import com.pista.estoque.spring.repository.orm.ReservaOrmMongo;

public interface ReservaRepositoryWithMongoDB extends MongoRepository<ReservaOrmMongo, String> {
    Optional<ReservaOrmMongo> findFirstBySessaoIdAndUsuarioIdAndStatus(String sessaoId, String usuarioId, StatusReserva status);
    List<ReservaOrmMongo> findByStatusAndExpiraEmBefore(StatusReserva status, Instant limite);
}
