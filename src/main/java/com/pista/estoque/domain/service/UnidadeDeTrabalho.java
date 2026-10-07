package com.pista.estoque.domain.service;

import java.util.function.Supplier;

// Executa o trabalho como uma transação ou tudo é gravado, ou nada
public interface UnidadeDeTrabalho {
    <T> T executar(Supplier<T> trabalho);
}
