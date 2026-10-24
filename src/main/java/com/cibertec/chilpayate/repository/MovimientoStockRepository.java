package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    List<MovimientoStock> findTop50ByOrderByFechaHoraDesc();
    List<MovimientoStock> findByProducto_IdProductoOrderByFechaHoraDesc(Integer idProducto);
}
