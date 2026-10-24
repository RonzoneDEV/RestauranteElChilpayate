package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.TicketCocina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCocinaRepository extends JpaRepository<TicketCocina, Integer> {
    List<TicketCocina> findByPedido_IdPedido(Integer idPedido);
    List<TicketCocina> findByEstado(TicketCocina.EstadoTicket estado);
    List<TicketCocina> findByEstadoInOrderByFechaHoraAsc(List<TicketCocina.EstadoTicket> estados);
}
