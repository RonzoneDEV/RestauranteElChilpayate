package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.ParametrosCarta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametrosCartaRepository extends JpaRepository<ParametrosCarta, Integer> {

    Optional<ParametrosCarta> findFirstByOrderByIdParametroAsc();

}
