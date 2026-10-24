package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEstadoInOrderByFechaDesc(Collection<Pedido.EstadoPedido> estados);
    List<Pedido> findByTipoAndEstadoInOrderByFechaDesc(Pedido.TipoPedido tipo, Collection<Pedido.EstadoPedido> estados);
    Optional<Pedido> findFirstByMesa_IdMesaAndEstadoInOrderByFechaDesc(Integer idMesa, Collection<Pedido.EstadoPedido> estados);
    List<Pedido> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);
    List<Pedido> findByEstadoOrderByFechaDesc(Pedido.EstadoPedido estado);
}
