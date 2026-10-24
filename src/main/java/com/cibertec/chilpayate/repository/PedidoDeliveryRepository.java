package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.PedidoDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoDeliveryRepository extends JpaRepository<PedidoDelivery, Integer> {
    Optional<PedidoDelivery> findByPedido_IdPedido(Integer idPedido);
    List<PedidoDelivery> findByEstadoOrderByFechaAsignacionDesc(PedidoDelivery.EstadoDelivery estado);
    List<PedidoDelivery> findAllByOrderByFechaAsignacionDesc();
}
