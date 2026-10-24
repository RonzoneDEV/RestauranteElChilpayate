package com.cibertec.chilpayate.repository;

import com.cibertec.chilpayate.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenteRepository extends JpaRepository<Incidente, Integer> {
    List<Incidente> findByPedido_IdPedido(Integer idPedido);
    List<Incidente> findAllByOrderByFechaHoraDesc();
    List<Incidente> findByTipoOrderByFechaHoraDesc(String tipo);
}
