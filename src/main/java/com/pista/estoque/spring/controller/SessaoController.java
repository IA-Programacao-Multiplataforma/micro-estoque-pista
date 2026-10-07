package com.pista.estoque.spring.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pista.estoque.domain.exception.NaoEncontradoException;
import com.pista.estoque.domain.repository.EventoRepository;
import com.pista.estoque.spring.controller.adapter.ReservaControllerAdapter;
import com.pista.estoque.spring.controller.dto.response.DisponibilidadeResponse;

@RestController
@RequestMapping("/sessoes")
public class SessaoController {

    private final EventoRepository repository;

    public SessaoController(EventoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{sessaoId}")
    public DisponibilidadeResponse disponibilidade(@PathVariable String sessaoId) {
        return repository.buscarPorSessaoId(sessaoId)
                .map(evento -> ReservaControllerAdapter.toDisponibilidade(evento, sessaoId, LocalDateTime.now()))
                .orElseThrow(() -> new NaoEncontradoException("Sessão não encontrada"));
    }
}
