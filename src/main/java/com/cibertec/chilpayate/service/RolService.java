package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Rol;
import com.cibertec.chilpayate.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RolService {

    private final RolRepository rolRepo;

    public RolService(RolRepository rolRepo) {
        this.rolRepo = rolRepo;
    }

    @Transactional(readOnly = true)
    public List<Rol> listarTodos() {
        return rolRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Rol> listarActivos() {
        return rolRepo.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Rol> obtenerPorId(Integer id) {
        return rolRepo.findById(id);
    }

    public boolean esRolSistema(String nombre) {
        if (nombre == null) return false;
        String n = nombre.trim().toUpperCase();
        return n.equals("ADMIN") || n.equals("ADMINISTRADOR")
                || n.equals("MOZO") || n.equals("CAJERO") || n.equals("CAJA")
                || n.equals("COCINERO") || n.equals("COCINA");
    }

    @Transactional
    public Rol guardar(Rol rol) {
        if (rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        String nombreLimpio = rol.getNombre().trim();
        if (rolRepo.findByNombreIgnoreCase(nombreLimpio).isPresent()) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre '" + nombreLimpio + "'");
        }
        rol.setNombre(nombreLimpio);
        if (rol.getEstado() == null) {
            rol.setEstado(true);
        }
        return rolRepo.save(rol);
    }

    @Transactional
    public Rol actualizar(Integer id, Rol datos) {
        Rol rol = rolRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con ID: " + id));

        if (esRolSistema(rol.getNombre())) {
            throw new IllegalStateException("El rol '" + rol.getNombre() + "' es un rol base del sistema y no puede ser modificado.");
        }

        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            rol.setNombre(datos.getNombre().trim());
        }
        if (datos.getEstado() != null) {
            rol.setEstado(datos.getEstado());
        }
        return rolRepo.save(rol);
    }

    @Transactional
    public void cambiarEstado(Integer id) {
        rolRepo.findById(id).ifPresent(r -> {
            if (esRolSistema(r.getNombre())) {
                throw new IllegalStateException("El rol '" + r.getNombre() + "' es un rol fundamental del sistema y no puede ser desactivado.");
            }
            r.setEstado(r.getEstado() == null || !r.getEstado());
            rolRepo.save(r);
        });
    }
}
