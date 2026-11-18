package com.pista.estoque.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.domain.entity.enumerable.StatusSessao;
import com.pista.estoque.domain.evento.PedidoCancelado;
import com.pista.estoque.domain.evento.PedidoCriado;
import com.pista.estoque.domain.evento.PedidoRealizado;
import com.pista.estoque.domain.exception.VendaRecusadaException;
import com.pista.estoque.domain.repository.EventoRepository;

/*
 * Recebe um pedido e decide: vende (REALIZADO) ou recusa (CANCELADO).
 *
 * Tudo acontece numa TRANSAÇÃO com LOCK PESSIMISTA no evento:
 *   1. trava o evento (nenhum outro pedido mexe nele até esta transação terminar);
 *   2. lê a quantidade disponível;
 *   3. se tiver para todos os itens, dá baixa; se faltar em algum, desfaz tudo (rollback).
 * Assim nunca se vende mais ingressos do que existem no banco.
 */
public class VendaService {

    private final EventoRepository eventos;
    private final UnidadeDeTrabalho transacao;
    private final PublicadorDeEventos publicador;
    private final int maxIngressosPorCompra;
    private final Clock clock;

    public VendaService(EventoRepository eventos, UnidadeDeTrabalho transacao, PublicadorDeEventos publicador,
                        int maxIngressosPorCompra, Clock clock) {
        this.eventos = eventos;
        this.transacao = transacao;
        this.publicador = publicador;
        this.maxIngressosPorCompra = maxIngressosPorCompra;
        this.clock = clock;
    }

    public void processar(PedidoCriado pedido) {
        try {
            PedidoRealizado realizado = transacao.executar(() -> vender(pedido));
            // Responde só depois do commit: o Pedido nunca recebe "realizado" de uma venda desfeita
            publicador.publicar(PedidoRealizado.ROUTING_KEY, realizado);
        } catch (VendaRecusadaException recusa) {
            publicador.publicar(PedidoCancelado.ROUTING_KEY, new PedidoCancelado(pedido.pedidoId(), recusa.getMessage()));
        }
    }

    private PedidoRealizado vender(PedidoCriado pedido) {
        Map<String, Integer> quantidadePorSetor = validar(pedido);

        // 1. LOCK: a partir daqui este evento é só desta transação
        if (!eventos.travarParaVenda(pedido.sessaoId())) {
            throw new VendaRecusadaException("Sessão não encontrada");
        }

        // 2. Lê o estoque já com a trava (ninguém mais está alterando)
        Evento evento = eventos.buscarPorSessaoId(pedido.sessaoId()).orElseThrow();
        Sessao sessao = evento.sessoes().stream()
                .filter(s -> s.id().equals(pedido.sessaoId()))
                .findFirst()
                .orElseThrow();

        if (sessao.status(LocalDateTime.now(clock)) == StatusSessao.ESTREIA) {
            throw new VendaRecusadaException("A venda desta sessão ainda não abriu");
        }

        // 3. Confere e dá baixa setor por setor; se um faltar, a exceção desfaz as baixas anteriores
        List<PedidoRealizado.Item> vendidos = new ArrayList<>();
        quantidadePorSetor.forEach((setorId, quantidade) -> {
            Setor setor = sessao.setores().stream()
                    .filter(s -> s.id().equals(setorId))
                    .findFirst()
                    .orElseThrow(() -> new VendaRecusadaException("Setor não encontrado"));

            if (setor.disponivel() < quantidade) {
                throw new VendaRecusadaException("Ingressos insuficientes no setor " + setor.nome());
            }
            eventos.darBaixa(evento.id(), sessao.id(), setor.id(), quantidade);
            vendidos.add(new PedidoRealizado.Item(setor.id(), setor.nome(), quantidade, setor.preco()));
        });

        return new PedidoRealizado(pedido.pedidoId(), evento.id(), evento.titulo(), sessao.dataEvento(), vendidos);
    }

    // Junta itens repetidos do mesmo setor e confere as quantidades
    private Map<String, Integer> validar(PedidoCriado pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new VendaRecusadaException("Pedido sem itens");
        }
        Map<String, Integer> porSetor = new LinkedHashMap<>();
        for (PedidoCriado.Item item : pedido.itens()) {
            if (item.quantidade() <= 0) {
                throw new VendaRecusadaException("Quantidade deve ser maior que zero");
            }
            porSetor.merge(item.setorId(), item.quantidade(), Integer::sum);
        }
        int total = porSetor.values().stream().mapToInt(Integer::intValue).sum();
        if (total > maxIngressosPorCompra) {
            throw new VendaRecusadaException("Máximo de " + maxIngressosPorCompra + " ingressos por compra");
        }
        return porSetor;
    }
}
