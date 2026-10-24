package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByUsernameAndEstadoTrue(String username);
    boolean existsByUsername(String username);
    List<Usuario> findByEstadoTrue();
}
