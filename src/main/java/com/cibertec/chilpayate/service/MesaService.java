package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Mesa;
import com.cibertec.chilpayate.repository.MesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MesaService {

    private final MesaRepository mesaRepo;

    public MesaService(MesaRepository mesaRepo) {
        this.mesaRepo = mesaRepo;
    }

    @Transactional(readOnly = true)
    public List<Mesa> listarTodas() {
        return mesaRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Mesa> listarActivas() {
        return mesaRepo.findByActivoTrueOrderByNumeroAsc();
    }

    @Transactional(readOnly = true)
    public List<Mesa> listarLibres() {
        return mesaRepo.findByEstadoAndActivoTrue(Mesa.EstadoMesa.LIBRE);
    }

    @Transactional(readOnly = true)
    public Optional<Mesa> obtenerPorId(Integer id) {
        return mesaRepo.findById(id);
    }

    @Transactional
    public Mesa guardar(Mesa m) {
        if (m.getNumero() == null) {
            throw new IllegalArgumentException("El número de mesa es obligatorio");
        }
        if (m.getNumero() <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser al menos 1");
        }

        String salon = (m.getSalon() == null || m.getSalon().isBlank()) ? "Salón Principal" : m.getSalon().trim();
        m.setSalon(salon);

        if (m.getIdMesa() == null) {
            if (mesaRepo.existsByNumeroAndSalonIgnoreCase(m.getNumero(), salon)) {
                throw new IllegalArgumentException("Ya existe la Mesa #" + m.getNumero() + " en el salón '" + salon + "'.");
            }
        } else {
            if (mesaRepo.existsByNumeroAndSalonIgnoreCaseAndIdMesaNot(m.getNumero(), salon, m.getIdMesa())) {
                throw new IllegalArgumentException("Ya existe otra mesa con el número #" + m.getNumero() + " en el salón '" + salon + "'.");
            }
        }

        if (m.getCapacidad() == null || m.getCapacidad() <= 0) {
            m.setCapacidad(4);
        }
        if (m.getEstado() == null) {
            m.setEstado(Mesa.EstadoMesa.LIBRE);
        }
        if (m.getActivo() == null) {
            m.setActivo(true);
        }
        return mesaRepo.save(m);
    }

    @Transactional
    public Mesa actualizar(Integer id, Mesa datos) {
        Mesa m = mesaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada: " + id));

        if (m.getEstado() == Mesa.EstadoMesa.OCUPADA) {
            throw new IllegalStateException("No se puede editar la Mesa #" + m.getNumero() + " (" + m.getSalon() + ") porque se encuentra actualmente OCUPADA en atención.");
        }

        if (datos.getNumero() == null) {
            throw new IllegalArgumentException("El número de mesa es obligatorio");
        }
        if (datos.getNumero() <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser al menos 1");
        }

        String salon = (datos.getSalon() != null && !datos.getSalon().isBlank()) ? datos.getSalon().trim() : "Salón Principal";

        if (mesaRepo.existsByNumeroAndSalonIgnoreCaseAndIdMesaNot(datos.getNumero(), salon, id)) {
            throw new IllegalArgumentException("Ya existe otra mesa con el número #" + datos.getNumero() + " en el salón '" + salon + "'.");
        }

        if (Boolean.FALSE.equals(datos.getActivo()) && m.getEstado() != Mesa.EstadoMesa.LIBRE) {
            throw new IllegalStateException("Solo se puede desactivar la mesa si su estado es LIBRE. Actualmente se encuentra " + m.getEstado() + ".");
        }

        m.setNumero(datos.getNumero());
        m.setCapacidad(datos.getCapacidad() != null && datos.getCapacidad() > 0 ? datos.getCapacidad() : 4);
        m.setSalon(salon);
        if (datos.getEstado() != null) {
            m.setEstado(datos.getEstado());
        }
        if (datos.getActivo() != null) {
            m.setActivo(datos.getActivo());
        }
        return mesaRepo.save(m);
    }

    @Transactional
    public void cambiarEstado(Integer idMesa, Mesa.EstadoMesa nuevoEstado) {
        mesaRepo.findById(idMesa).ifPresent(m -> {
            m.setEstado(nuevoEstado);
            mesaRepo.save(m);
        });
    }

    @Transactional
    public void cambiarActivo(Integer id) {
        Mesa m = mesaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada: " + id));

        boolean vaADesactivar = Boolean.TRUE.equals(m.getActivo());
        if (vaADesactivar && m.getEstado() != Mesa.EstadoMesa.LIBRE) {
            throw new IllegalStateException("Solo se puede desactivar una mesa cuando su estado sea LIBRE. La Mesa #" + 
                    m.getNumero() + " (" + m.getSalon() + ") está actualmente " + m.getEstado() + ".");
        }

        m.setActivo(!vaADesactivar);
        mesaRepo.save(m);
    }
}
