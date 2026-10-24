package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    List<Cliente> findByEstadoTrue();
    Optional<Cliente> findByTelefono(String telefono);
    boolean existsByTelefono(String telefono);
    boolean existsByNombreIgnoreCase(String nombre);
    List<Cliente> findByNombreContainingIgnoreCaseOrTelefonoContaining(String nombre, String telefono);
}
