package com.pista.estoque.spring.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pista.estoque.domain.entity.Evento;
import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.spring.controller.adapter.EventoControllerAdapter;
import com.pista.estoque.spring.controller.dto.request.EventoRequest;
import com.pista.estoque.spring.controller.dto.request.SessaoRequest;
import com.pista.estoque.spring.controller.dto.request.SetorAjusteRequest;
import com.pista.estoque.spring.controller.dto.response.EventoResponse;

import jakarta.validation.Valid;

// Autorização (GET público, resto só ADMIN) fica no gateway, que também remove o prefixo "/estoque"
@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoRepository repository;

    public EventoController(EventoRepository repository) {
        this.repository = repository;
    }

    private Evento buscar(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Evento não encontrado"));
    }

    private EventoResponse resposta(Evento evento) {
        return EventoControllerAdapter.toResponse(evento, LocalDateTime.now());
    }

    @GetMapping
    public List<EventoResponse> listar() {
        return repository.listar().stream().map(this::resposta).toList();
    }

    @GetMapping("/{id}")
    public EventoResponse detalhar(@PathVariable String id) {
        return resposta(buscar(id));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventoResponse criar(@Valid @RequestBody EventoRequest request) {
        Evento novo = EventoControllerAdapter.cast(request);
        return resposta(repository.salvar(novo));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/sessoes")
    public EventoResponse adicionarSessao(@PathVariable String id, @Valid @RequestBody SessaoRequest request) {
        Evento atualizado = buscar(id).comNovaSessao(EventoControllerAdapter.cast(request));
        return resposta(repository.salvar(atualizado));
    }

    @PutMapping("/{id}/sessoes/{sessaoId}/setores/{setorId}")
    public EventoResponse ajustarSetor(
            @PathVariable String id,
            @PathVariable String sessaoId,
            @PathVariable String setorId,
            @RequestBody SetorAjusteRequest request) {
        Evento atualizado = buscar(id).comSetorAjustado(sessaoId, setorId, request.capacidade(), request.preco());
        return resposta(repository.salvar(atualizado));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable String id) {
        buscar(id).validarExclusao();
        repository.deletar(id);
    }
}
