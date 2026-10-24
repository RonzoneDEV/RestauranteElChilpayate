package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {
    List<DetallePedido> findByPedido_IdPedido(Integer idPedido);
    List<DetallePedido> findByEstado(DetallePedido.EstadoDetalle estado);
    List<DetallePedido> findByEstadoIn(Collection<DetallePedido.EstadoDetalle> estados);
    List<DetallePedido> findByTicketCocina_IdTicket(Integer idTicket);

    @Query("SELECT d FROM DetallePedido d " +
           "JOIN FETCH d.producto p " +
           "LEFT JOIN FETCH p.categoria c " +
           "JOIN FETCH d.pedido ped " +
           "LEFT JOIN FETCH ped.usuario u " +
           "WHERE ped.fecha BETWEEN :desde AND :hasta " +
           "AND ped.estado != 'ANULADA' AND d.estado != 'ANULADO'")
    List<DetallePedido> findDetallesValidosEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
