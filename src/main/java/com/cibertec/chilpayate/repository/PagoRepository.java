package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {
    List<Pago> findByPedido_IdPedido(Integer idPedido);
    List<Pago> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);
}
