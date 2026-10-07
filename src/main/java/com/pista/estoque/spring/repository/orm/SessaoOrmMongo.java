package com.pista.estoque.spring.repository.orm;

import java.time.LocalDateTime;
import java.util.List;

public record SessaoOrmMongo(
        String id,
        LocalDateTime dataEvento,
        LocalDateTime inicioVenda,
        List<SetorOrmMongo> setores
) {
}
