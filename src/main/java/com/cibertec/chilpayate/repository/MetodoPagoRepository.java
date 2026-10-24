package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {
    List<MetodoPago> findByEstadoTrue();
    List<MetodoPago> findByNombreContainingIgnoreCaseAndEstadoTrue(String nombre);
}
