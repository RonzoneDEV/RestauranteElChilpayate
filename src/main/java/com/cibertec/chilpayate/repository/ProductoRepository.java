package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByEstadoTrue();
    List<Producto> findByCategoria_IdCategoriaAndEstadoTrue(Integer idCategoria);
    List<Producto> findByPermiteSalsaTrueAndEstadoTrue();
    List<Producto> findByNombreContainingIgnoreCaseAndEstadoTrue(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);

    @Query("SELECT p FROM Producto p WHERE p.controlaStock = true AND p.stock <= :limite AND p.estado = true")
    List<Producto> findStockBajo(@Param("limite") int limite);
}
