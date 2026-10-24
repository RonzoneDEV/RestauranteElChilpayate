package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {
    List<Venta> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);
    Optional<Venta> findByPedido_IdPedido(Integer idPedido);
    Optional<Venta> findByPrecuenta_IdPrecuenta(Integer idPrecuenta);
    List<Venta> findByPedido_IdPedidoOrderByFechaHoraDesc(Integer idPedido);
}
