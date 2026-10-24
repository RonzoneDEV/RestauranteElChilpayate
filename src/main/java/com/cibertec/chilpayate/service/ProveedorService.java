package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Proveedor;
import com.cibertec.chilpayate.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepo;

    public ProveedorService(ProveedorRepository proveedorRepo) {
        this.proveedorRepo = proveedorRepo;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarTodos() {
        return proveedorRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarActivos() {
        return proveedorRepo.findByEstadoTrueOrderByRazonSocialAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Proveedor> obtenerPorId(Integer id) {
        return proveedorRepo.findById(id);
    }

    @Transactional
    public Proveedor guardar(Proveedor p) {
        if (p.getRazonSocial() == null || p.getRazonSocial().isBlank()) {
            throw new IllegalArgumentException("La razón social es obligatoria");
        }
        if (p.getEstado() == null) p.setEstado(true);
        return proveedorRepo.save(p);
    }

    @Transactional
    public Proveedor actualizar(Integer id, Proveedor p) {
        Proveedor existente = proveedorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + id));
        existente.setRazonSocial(p.getRazonSocial());
        existente.setRuc(p.getRuc());
        existente.setTelefono(p.getTelefono());
        existente.setDireccion(p.getDireccion());
        if (p.getEstado() != null) {
            existente.setEstado(p.getEstado());
        }
        return proveedorRepo.save(existente);
    }

    @Transactional
    public void cambiarEstado(Integer id) {
        proveedorRepo.findById(id).ifPresent(p -> {
            p.setEstado(p.getEstado() == null || !p.getEstado());
            proveedorRepo.save(p);
        });
    }
}
