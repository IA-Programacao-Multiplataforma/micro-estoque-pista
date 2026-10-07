package com.pista.estoque.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.pista.estoque.domain.entity.Reserva;
import com.pista.estoque.domain.entity.enumerable.StatusReserva;

public interface ReservaRepository {
    Reserva salvar(Reserva reserva);

    Optional<Reserva> buscarPorId(String id);
    Optional<Reserva> buscarAtivaPorSessaoEUsuario(String sessaoId, String usuarioId);
    List<Reserva> listarAtivasVencidasAte(Instant limite);

    // Troca o status só se ainda estiver em "de"; true = esta chamada venceu (impede devolver os mesmos ingressos duas vezes)
    boolean transicionar(String id, StatusReserva de, StatusReserva para);
}
