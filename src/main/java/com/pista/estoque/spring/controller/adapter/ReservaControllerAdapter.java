package com.pista.estoque.spring.controller.adapter;

import java.time.LocalDateTime;
import java.util.List;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Reserva;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.service.ItemSolicitado;
import com.pista.estoque.spring.controller.dto.request.ItemReservaRequest;
import com.pista.estoque.spring.controller.dto.response.DisponibilidadeResponse;
import com.pista.estoque.spring.controller.dto.response.ItemReservaResponse;
import com.pista.estoque.spring.controller.dto.response.ReservaResponse;

public class ReservaControllerAdapter {

    private ReservaControllerAdapter() {}

    public static List<ItemSolicitado> cast(List<ItemReservaRequest> itens) {
        return itens.stream().map(i -> new ItemSolicitado(i.setorId(), i.quantidade())).toList();
    }

    public static ReservaResponse toResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.id(),
                reserva.sessaoId(),
                reserva.eventoId(),
                reserva.usuarioId(),
                reserva.status(),
                reserva.expiraEm(),
                reserva.itens().stream()
                        .map(i -> new ItemReservaResponse(i.setorId(), i.setorNome(), i.quantidade(),
                                i.precoUnitario(), i.subtotal()))
                        .toList(),
                reserva.total()
        );
    }

    public static DisponibilidadeResponse toDisponibilidade(Evento evento, String sessaoId, LocalDateTime agora) {
        Sessao sessao = evento.sessoes().stream()
                .filter(s -> s.id().equals(sessaoId))
                .findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Sessão não encontrada"));

        return new DisponibilidadeResponse(
                sessao.id(),
                evento.id(),
                evento.titulo(),
                sessao.dataEvento(),
                sessao.inicioVenda(),
                sessao.status(agora),
                sessao.setores().stream()
                        .map(s -> new DisponibilidadeResponse.SetorDisponivel(s.id(), s.nome(), s.preco(), s.disponivel()))
                        .toList()
        );
    }
}
