package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CierreCajaRepository extends JpaRepository<CierreCaja, Integer> {
    Optional<CierreCaja> findTopByOrderByFechaAperturaDesc();
    List<CierreCaja> findByFechaAperturaBetweenOrderByFechaAperturaDesc(LocalDateTime desde, LocalDateTime hasta);
    List<CierreCaja> findAllByOrderByFechaAperturaDesc();
}
