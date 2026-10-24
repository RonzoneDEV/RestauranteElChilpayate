package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.PrecuentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecuentaDetalleRepository extends JpaRepository<PrecuentaDetalle, Integer> {
    List<PrecuentaDetalle> findByPrecuenta_IdPrecuenta(Integer idPrecuenta);
    void deleteByPrecuenta_IdPrecuenta(Integer idPrecuenta);
}
