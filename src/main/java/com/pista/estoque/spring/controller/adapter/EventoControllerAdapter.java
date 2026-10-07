package com.pista.estoque.spring.controller.adapter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.entity.Sessao;
import com.pista.estoque.domain.entity.Setor;
import com.pista.estoque.spring.controller.dto.request.EventoRequest;
import com.pista.estoque.spring.controller.dto.request.SessaoRequest;
import com.pista.estoque.spring.controller.dto.request.SetorRequest;
import com.pista.estoque.spring.controller.dto.response.EventoResponse;
import com.pista.estoque.spring.controller.dto.response.SessaoResponse;
import com.pista.estoque.spring.controller.dto.response.SetorResponse;

public class EventoControllerAdapter {

    private EventoControllerAdapter() {}

    // Ids e contadores de estoque são gerados aqui; o cliente nunca os envia
    public static Evento cast(EventoRequest request) {
        return new Evento(
                UUID.randomUUID().toString(),
                request.titulo().trim(),
                request.descricao(),
                request.categoria(),
                request.local().trim(),
                request.cidade().trim(),
                request.imagemUrl(),
                request.imagemAlt(),
                request.sessoes().stream().map(EventoControllerAdapter::cast).toList()
        );
    }

    public static Sessao cast(SessaoRequest request) {
        return new Sessao(
                UUID.randomUUID().toString(),
                request.dataEvento(),
                request.inicioVenda(),
                request.setores().stream().map(EventoControllerAdapter::cast).toList()
        );
    }

    private static Setor cast(SetorRequest request) {
        return Setor.novo(
                UUID.randomUUID().toString(),
                request.nome().trim(),
                request.preco(),
                request.capacidade()
        );
    }

    public static EventoResponse toResponse(Evento evento, LocalDateTime agora) {
        return new EventoResponse(
                evento.id(),
                evento.titulo(),
                evento.descricao(),
                evento.categoria(),
                evento.local(),
                evento.cidade(),
                evento.imagemUrl(),
                evento.imagemAlt(),
                evento.sessoes().stream().map(s -> toResponse(s, agora)).toList()
        );
    }

    private static SessaoResponse toResponse(Sessao sessao, LocalDateTime agora) {
        return new SessaoResponse(
                sessao.id(),
                sessao.dataEvento(),
                sessao.inicioVenda(),
                sessao.status(agora),
                sessao.setores().stream().map(EventoControllerAdapter::toResponse).toList()
        );
    }

    private static SetorResponse toResponse(Setor setor) {
        return new SetorResponse(
                setor.id(),
                setor.nome(),
                setor.preco(),
                setor.capacidade(),
                setor.disponivel(),
                setor.reservado(),
                setor.vendido()
        );
    }
}
