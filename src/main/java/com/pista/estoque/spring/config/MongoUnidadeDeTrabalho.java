package com.pista.estoque.spring.config;

import java.util.function.Supplier;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.mongodb.MongoException;
import com.pista.estoque.domain.service.UnidadeDeTrabalho;

// Repete a transação quando o Mongo avisa colisão ("TransientTransactionError"); sem isso, confirmações simultâneas falhariam à toa
public class MongoUnidadeDeTrabalho implements UnidadeDeTrabalho {

    private static final int MAX_TENTATIVAS = 5;

    private final TransactionTemplate template;

    public MongoUnidadeDeTrabalho(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T executar(Supplier<T> trabalho) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return template.execute(status -> trabalho.get());
            } catch (RuntimeException e) {
                if (tentativa >= MAX_TENTATIVAS || !transitorio(e)) {
                    throw e;
                }
                pausar(tentativa);
            }
        }
    }

    private static boolean transitorio(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof MongoException mongo
                    && (mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                        || mongo.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL))) {
                return true;
            }
        }
        return false;
    }

    private static void pausar(int tentativa) {
        try {
            Thread.sleep(20L * tentativa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
