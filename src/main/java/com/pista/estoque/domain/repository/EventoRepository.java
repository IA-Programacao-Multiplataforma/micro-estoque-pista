package com.pista.estoque.domain.repository;

import java.util.List;
import java.util.Optional;

import com.pista.estoque.domain.entity.Evento;

public interface EventoRepository {
    Evento salvar(Evento evento);
    void deletar(String id);

    Optional<Evento> buscarPorId(String id);
    Optional<Evento> buscarPorSessaoId(String sessaoId);
    List<Evento> listar();

    // Trava o evento (pelo id) até o fim da transação, como a venda faz. false = evento não existe
    boolean travarEvento(String eventoId);

    // Lock pessimista: trava o evento da sessão até a transação terminar. false = sessão não existe
    boolean travarParaVenda(String sessaoId);

    // Tira "quantidade" do disponível e soma no vendido (chamar com o evento já travado)
    void darBaixa(String eventoId, String sessaoId, String setorId, int quantidade);
}
