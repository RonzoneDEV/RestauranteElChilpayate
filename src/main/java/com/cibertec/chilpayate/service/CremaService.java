package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Crema;
import com.cibertec.chilpayate.repository.CremaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CremaService {

    private final CremaRepository cremaRepo;

    public CremaService(CremaRepository cremaRepo) {
        this.cremaRepo = cremaRepo;
    }

    @Transactional(readOnly = true)
    public List<Crema> listarTodos() {
        return cremaRepo.findAll();
    }

    @Cacheable(value = "cremasActivas")
    @Transactional(readOnly = true)
    public List<Crema> listarActivos() {
        return cremaRepo.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Crema> obtenerPorId(Integer id) {
        return cremaRepo.findById(id);
    }

    @CacheEvict(value = "cremasActivas", allEntries = true)
    @Transactional
    public Crema guardar(Crema c) {
        if (c.getNombre() == null || c.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la crema es obligatorio");
        }
        if (c.getEstado() == null) c.setEstado(true);
        return cremaRepo.save(c);
    }

    @CacheEvict(value = "cremasActivas", allEntries = true)
    @Transactional
    public Crema actualizar(Integer id, Crema c) {
        Crema existente = cremaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Crema no encontrada: " + id));
        existente.setNombre(c.getNombre());
        if (c.getEstado() != null) {
            existente.setEstado(c.getEstado());
        }
        return cremaRepo.save(existente);
    }

    @CacheEvict(value = "cremasActivas", allEntries = true)
    @Transactional
    public void cambiarEstado(Integer id) {
        cremaRepo.findById(id).ifPresent(c -> {
            c.setEstado(c.getEstado() == null || !c.getEstado());
            cremaRepo.save(c);
        });
    }
}
