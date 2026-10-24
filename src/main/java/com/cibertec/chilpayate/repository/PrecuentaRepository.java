package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Precuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecuentaRepository extends JpaRepository<Precuenta, Integer> {
    List<Precuenta> findByPedido_IdPedidoOrderByNumeroPrecuentaAsc(Integer idPedido);
    List<Precuenta> findByPedido_IdPedidoAndEstado(Integer idPedido, Precuenta.EstadoPrecuenta estado);
    List<Precuenta> findByPedido_IdPedidoAndEstadoNot(Integer idPedido, Precuenta.EstadoPrecuenta estado);
    List<Precuenta> findByPedido_IdPedidoAndEstadoNotOrderByNumeroPrecuentaAsc(Integer idPedido, Precuenta.EstadoPrecuenta estado);
    boolean existsByPedido_IdPedidoAndEstado(Integer idPedido, Precuenta.EstadoPrecuenta estado);
}
