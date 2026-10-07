package com.pista.estoque.spring.repository.adapter;

import com.pista.estoque.domain.entity.ItemReserva;
import com.pista.estoque.domain.entity.Reserva;
import com.pista.estoque.spring.repository.orm.ItemReservaOrmMongo;
import com.pista.estoque.spring.repository.orm.ReservaOrmMongo;

public class ReservaRepositoryAdapter {
    private ReservaRepositoryAdapter() {}

    public static Reserva castOrm(ReservaOrmMongo orm) {
        return new Reserva(
                orm.id(), orm.sessaoId(), orm.eventoId(), orm.usuarioId(), orm.status(),
                orm.expiraEm(), orm.criadaEm(),
                orm.itens().stream()
                        .map(i -> new ItemReserva(i.setorId(), i.setorNome(), i.quantidade(), i.precoUnitario()))
                        .toList());
    }

    public static ReservaOrmMongo castEntity(Reserva entity) {
        return new ReservaOrmMongo(
                entity.id(), entity.sessaoId(), entity.eventoId(), entity.usuarioId(), entity.status(),
                entity.expiraEm(), entity.criadaEm(),
                entity.itens().stream()
                        .map(i -> new ItemReservaOrmMongo(i.setorId(), i.setorNome(), i.quantidade(), i.precoUnitario()))
                        .toList());
    }
}
