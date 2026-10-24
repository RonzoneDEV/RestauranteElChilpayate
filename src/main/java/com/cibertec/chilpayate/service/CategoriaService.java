package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Categoria;
import com.cibertec.chilpayate.repository.CategoriaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodos() {
        return categoriaRepository.findAll();
    }

    @Cacheable(value = "categoriasActivas")
    @Transactional(readOnly = true)
    public List<Categoria> listarActivos() {
        return categoriaRepository.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Categoria> obtenerPorId(Integer id) {
        return categoriaRepository.findById(id);
    }

    public boolean esCategoriaProtegida(String nombre) {
        if (nombre == null) return false;
        String n = nombre.trim().toUpperCase();
        return n.equals("ALITAS") || n.equals("ALITA") || n.equals("SALSA") || n.equals("SALSAS");
    }

    @CacheEvict(value = "categoriasActivas", allEntries = true)
    @Transactional
    public Categoria guardar(Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        String nombreLimpio = categoria.getNombre().trim();

        if (esCategoriaProtegida(nombreLimpio)) {
            throw new IllegalStateException("No se puede registrar otra categoría con el nombre protegido '" + nombreLimpio + "'.");
        }

        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre '" + nombreLimpio + "'.");
        }

        categoria.setNombre(nombreLimpio);
        if (categoria.getEstado() == null) {
            categoria.setEstado(true);
        }
        return categoriaRepository.save(categoria);
    }

    @CacheEvict(value = "categoriasActivas", allEntries = true)
    @Transactional
    public Categoria actualizar(Integer id, Categoria datos) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));

        if (esCategoriaProtegida(c.getNombre())) {
            throw new IllegalStateException("La categoría '" + c.getNombre() + "' es núcleo del sistema y no puede ser modificada ni desactivada.");
        }

        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío.");
        }

        String nuevoNombre = datos.getNombre().trim();

        if (esCategoriaProtegida(nuevoNombre)) {
            throw new IllegalStateException("No se puede asignar el nombre protegido '" + nuevoNombre + "' a una categoría estándar.");
        }

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(nuevoNombre, id)) {
            throw new IllegalArgumentException("Ya existe otra categoría registrada con el nombre '" + nuevoNombre + "'.");
        }

        c.setNombre(nuevoNombre);
        if (datos.getEstado() != null) {
            c.setEstado(datos.getEstado());
        }
        return categoriaRepository.save(c);
    }

    @CacheEvict(value = "categoriasActivas", allEntries = true)
    @Transactional
    public void cambiarEstado(Integer id) {
        categoriaRepository.findById(id).ifPresent(c -> {
            if (esCategoriaProtegida(c.getNombre()) && Boolean.TRUE.equals(c.getEstado())) {
                throw new IllegalStateException("La categoría '" + c.getNombre() + "' no puede ser desactivada.");
            }
            c.setEstado(c.getEstado() == null || !c.getEstado());
            categoriaRepository.save(c);
        });
    }
}
