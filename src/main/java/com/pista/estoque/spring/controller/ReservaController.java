package com.pista.estoque.spring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pista.estoque.domain.service.ReservaService;
import com.pista.estoque.spring.controller.adapter.ReservaControllerAdapter;
import com.pista.estoque.spring.controller.dto.request.CriarReservaRequest;
import com.pista.estoque.spring.controller.dto.response.ReservaResponse;

import jakarta.validation.Valid;

// INTERNO: só o serviço de fila chama estas rotas; o gateway bloqueia /estoque/reservas/** (denyAll)
@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReservaResponse criar(@Valid @RequestBody CriarReservaRequest request) {
        return ReservaControllerAdapter.toResponse(service.criarOuSubstituir(
                request.sessaoId(),
                request.usuarioId(),
                request.expiraEm(),
                ReservaControllerAdapter.cast(request.itens())));
    }

    @GetMapping("/{id}")
    public ReservaResponse consultar(@PathVariable String id) {
        return ReservaControllerAdapter.toResponse(service.buscar(id));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/liberar")
    public void liberar(@PathVariable String id) {
        service.liberar(id);
    }
}
