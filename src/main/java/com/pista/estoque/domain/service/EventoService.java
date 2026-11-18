package com.pista.estoque.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.domain.entity.enumerable.Categoria;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.repository.EventoRepository;

/*
 * Alterações no cadastro do evento (dados, datas e setores) feitas pelo administrador.
 *
 * Cada alteração roda numa TRANSAÇÃO que primeiro TRAVA o evento: é a mesma trava que a venda usa
 * (VendaService). Assim, uma edição nunca sobrescreve uma venda em andamento (a gravação troca o
 * documento inteiro), e uma venda nunca acontece no meio de uma exclusão.
 */
public class EventoService {

    private final EventoRepository eventos;
    private final UnidadeDeTrabalho transacao;

    public EventoService(EventoRepository eventos, UnidadeDeTrabalho transacao) {
        this.eventos = eventos;
        this.transacao = transacao;
    }

    public Evento atualizarEvento(String eventoId, String titulo, String descricao, Categoria categoria,
                                  String local, String cidade, String imagemUrl, String imagemAlt) {
        return alterar(eventoId, e -> e.comDadosEditados(titulo, descricao, categoria, local, cidade, imagemUrl, imagemAlt));
    }

    public Evento adicionarSessao(String eventoId, Sessao sessao) {
        return alterar(eventoId, e -> e.comNovaSessao(sessao));
    }

    public Evento atualizarSessao(String eventoId, String sessaoId, LocalDateTime dataEvento, LocalDateTime inicioVenda) {
        return alterar(eventoId, e -> e.comSessaoEditada(sessaoId, dataEvento, inicioVenda));
    }

    public Evento excluirSessao(String eventoId, String sessaoId) {
        return alterar(eventoId, e -> e.semSessao(sessaoId));
    }

    public Evento adicionarSetor(String eventoId, String sessaoId, String nome, BigDecimal preco, int capacidade) {
        Setor novo = Setor.novo(UUID.randomUUID().toString(), nome.trim(), preco, capacidade);
        return alterar(eventoId, e -> e.comSetorNaSessao(sessaoId, novo));
    }

    public Evento atualizarSetor(String eventoId, String sessaoId, String setorId, String nome,
                                 Integer capacidade, BigDecimal preco) {
        return alterar(eventoId, e -> e.comSetorAjustado(sessaoId, setorId, nome, capacidade, preco));
    }

    public Evento excluirSetor(String eventoId, String sessaoId, String setorId) {
        return alterar(eventoId, e -> e.semSetor(sessaoId, setorId));
    }

    // A conferência de "tem venda?" e a exclusão acontecem com o evento travado
    public void excluirEvento(String eventoId) {
        transacao.executar(() -> {
            Evento atual = travarEBuscar(eventoId);
            atual.validarExclusao();
            eventos.deletar(eventoId);
            return null;
        });
    }

    private Evento alterar(String eventoId, UnaryOperator<Evento> mudanca) {
        return transacao.executar(() -> eventos.salvar(mudanca.apply(travarEBuscar(eventoId))));
    }

    private Evento travarEBuscar(String eventoId) {
        if (!eventos.travarEvento(eventoId)) {
            throw new NaoEncontradoException("Evento não encontrado");
        }
        return eventos.buscarPorId(eventoId).orElseThrow(() -> new NaoEncontradoException("Evento não encontrado"));
    }
}
