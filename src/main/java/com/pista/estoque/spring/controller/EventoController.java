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
import com.pista.estoque.domain.service.EventoService;
import com.pista.estoque.spring.controller.adapter.EventoControllerAdapter;
import com.pista.estoque.spring.controller.dto.request.EventoEdicaoRequest;
import com.pista.estoque.spring.controller.dto.request.EventoRequest;
import com.pista.estoque.spring.controller.dto.request.SessaoEdicaoRequest;
import com.pista.estoque.spring.controller.dto.request.SessaoRequest;
import com.pista.estoque.spring.controller.dto.request.SetorAjusteRequest;
import com.pista.estoque.spring.controller.dto.request.SetorRequest;
import com.pista.estoque.spring.controller.dto.response.EventoResponse;

import jakarta.validation.Valid;

// Autorização (GET público, resto só ADMIN) fica no gateway, que também remove o prefixo "/estoque".
// As alterações passam pelo EventoService, que trava o evento (mesma trava da venda).
@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoRepository repository;
    private final EventoService service;

    public EventoController(EventoRepository repository, EventoService service) {
        this.repository = repository;
        this.service = service;
    }

    private Evento buscar(String id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Evento não encontrado"));
    }

    private EventoResponse resposta(Evento evento) {
        return EventoControllerAdapter.toResponse(evento, LocalDateTime.now());
    }

    // ---------------------------------------------------------------- consulta

    @GetMapping
    public List<EventoResponse> listar() {
        return repository.listar().stream().map(this::resposta).toList();
    }

    @GetMapping("/{id}")
    public EventoResponse detalhar(@PathVariable String id) {
        return resposta(buscar(id));
    }

    // ---------------------------------------------------------------- evento

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventoResponse criar(@Valid @RequestBody EventoRequest request) {
        Evento novo = EventoControllerAdapter.cast(request);
        return resposta(repository.salvar(novo));
    }

    // Edita título, descrição, categoria, local, cidade e imagem
    @PutMapping("/{id}")
    public EventoResponse editar(@PathVariable String id, @Valid @RequestBody EventoEdicaoRequest request) {
        return resposta(service.atualizarEvento(id, request.titulo(), request.descricao(), request.categoria(),
                request.local(), request.cidade(), request.imagemUrl(), request.imagemAlt()));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable String id) {
        service.excluirEvento(id);
    }

    // ---------------------------------------------------------------- datas (sessões)

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/sessoes")
    public EventoResponse adicionarSessao(@PathVariable String id, @Valid @RequestBody SessaoRequest request) {
        return resposta(service.adicionarSessao(id, EventoControllerAdapter.cast(request)));
    }

    // Troca a data do evento e o início da venda
    @PutMapping("/{id}/sessoes/{sessaoId}")
    public EventoResponse editarSessao(
            @PathVariable String id,
            @PathVariable String sessaoId,
            @Valid @RequestBody SessaoEdicaoRequest request) {
        return resposta(service.atualizarSessao(id, sessaoId, request.dataEvento(), request.inicioVenda()));
    }

    // Recusa se já há ingressos vendidos na data ou se for a única data do evento
    @DeleteMapping("/{id}/sessoes/{sessaoId}")
    public EventoResponse excluirSessao(@PathVariable String id, @PathVariable String sessaoId) {
        return resposta(service.excluirSessao(id, sessaoId));
    }

    // ---------------------------------------------------------------- setores (tipos de ingresso)

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/sessoes/{sessaoId}/setores")
    public EventoResponse adicionarSetor(
            @PathVariable String id,
            @PathVariable String sessaoId,
            @Valid @RequestBody SetorRequest request) {
        return resposta(service.adicionarSetor(id, sessaoId, request.nome(), request.preco(), request.capacidade()));
    }

    // Só o que for informado muda (nome, capacidade e/ou preço)
    @PutMapping("/{id}/sessoes/{sessaoId}/setores/{setorId}")
    public EventoResponse ajustarSetor(
            @PathVariable String id,
            @PathVariable String sessaoId,
            @PathVariable String setorId,
            @RequestBody SetorAjusteRequest request) {
        return resposta(service.atualizarSetor(id, sessaoId, setorId, request.nome(), request.capacidade(), request.preco()));
    }

    // Recusa se já há ingressos vendidos no setor ou se for o único setor da data
    @DeleteMapping("/{id}/sessoes/{sessaoId}/setores/{setorId}")
    public EventoResponse excluirSetor(
            @PathVariable String id,
            @PathVariable String sessaoId,
            @PathVariable String setorId) {
        return resposta(service.excluirSetor(id, sessaoId, setorId));
    }
}
