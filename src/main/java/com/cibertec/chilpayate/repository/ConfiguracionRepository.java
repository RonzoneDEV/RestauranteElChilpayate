package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Integer> {
    Optional<Configuracion> findTopByOrderByIdConfiguracionAsc();
}
