package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.DetallePedidoSalsa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetallePedidoSalsaRepository extends JpaRepository<DetallePedidoSalsa, Integer> {
    List<DetallePedidoSalsa> findByDetallePedido_IdDetallePedido(Integer idDetallePedido);

    @Query("SELECT s FROM DetallePedidoSalsa s " +
           "JOIN FETCH s.salsa sp " +
           "JOIN FETCH s.detallePedido d " +
           "JOIN FETCH d.pedido ped " +
           "WHERE ped.fecha BETWEEN :desde AND :hasta " +
           "AND ped.estado != 'ANULADA' AND d.estado != 'ANULADO'")
    List<DetallePedidoSalsa> findSalsasValidasEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
