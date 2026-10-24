package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.PagoMedio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoMedioRepository extends JpaRepository<PagoMedio, Integer> {
    List<PagoMedio> findByPago_IdPago(Integer idPago);

    @Query("SELECT pm FROM PagoMedio pm " +
           "JOIN FETCH pm.metodoPago mp " +
           "JOIN FETCH pm.pago p " +
           "JOIN FETCH p.pedido ped " +
           "WHERE ped.fecha BETWEEN :desde AND :hasta " +
           "AND ped.estado != 'ANULADA'")
    List<PagoMedio> findPagosMediosEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
