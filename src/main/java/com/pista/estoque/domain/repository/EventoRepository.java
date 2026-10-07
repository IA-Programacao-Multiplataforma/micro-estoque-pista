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


    boolean reservarIngressos(String eventoId, String sessaoId, String setorId, int quantidade);

    void devolverIngressos(String eventoId, String sessaoId, String setorId, int quantidade);

    void confirmarVenda(String eventoId, String sessaoId, String setorId, int quantidade);
}
