package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.PagoDetalle;
import com.cibertec.chilpayate.model.PagoDetalleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoDetalleRepository extends JpaRepository<PagoDetalle, PagoDetalleId> {
    List<PagoDetalle> findByPago_IdPago(Integer idPago);

    @Modifying
    @Query(value = "INSERT IGNORE INTO pago_detalle (id_pago, id_detalle_pedido) VALUES (:idPago, :idDetallePedido)", nativeQuery = true)
    void insertarPagoDetalle(@Param("idPago") Integer idPago, @Param("idDetallePedido") Integer idDetallePedido);
}
