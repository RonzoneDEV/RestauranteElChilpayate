package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Crema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CremaRepository extends JpaRepository<Crema, Integer> {
    List<Crema> findByEstadoTrue();
    Optional<Crema> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
