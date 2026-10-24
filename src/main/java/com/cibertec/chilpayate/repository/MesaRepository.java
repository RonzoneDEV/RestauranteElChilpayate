package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Integer> {
    List<Mesa> findByActivoTrueOrderByNumeroAsc();
    List<Mesa> findByEstadoAndActivoTrue(Mesa.EstadoMesa estado);
    Optional<Mesa> findByNumero(Integer numero);
    Optional<Mesa> findByNumeroAndSalonIgnoreCase(Integer numero, String salon);
    boolean existsByNumero(Integer numero);
    boolean existsByNumeroAndIdMesaNot(Integer numero, Integer idMesa);
    boolean existsByNumeroAndSalonIgnoreCase(Integer numero, String salon);
    boolean existsByNumeroAndSalonIgnoreCaseAndIdMesaNot(Integer numero, String salon, Integer idMesa);
}
