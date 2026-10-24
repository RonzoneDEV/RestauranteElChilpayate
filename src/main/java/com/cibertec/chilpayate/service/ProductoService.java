package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Producto;
import com.cibertec.chilpayate.repository.CategoriaRepository;
import com.cibertec.chilpayate.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public ProductoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {
        return productoRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarActivos() {
        return productoRepo.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(Integer idCategoria) {
        return productoRepo.findByCategoria_IdCategoriaAndEstadoTrue(idCategoria);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarSalsas() {
        return productoRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEstado()))
                .filter(p -> p.getCategoria() != null &&
                        (p.getCategoria().getNombre().equalsIgnoreCase("SALSA") ||
                                p.getCategoria().getNombre().equalsIgnoreCase("SALSAS")))
                .filter(p -> p.getNombre() == null || !p.getNombre().toLowerCase().contains("alita"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarStockBajo(int limite) {
        return productoRepo.findStockBajo(limite);
    }

    @Transactional(readOnly = true)
    public Optional<Producto> obtenerPorId(Integer id) {
        return productoRepo.findById(id);
    }

    @Transactional
    public Producto guardar(Producto p) {
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        if (p.getPrecio() == null || p.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (p.getCategoria() != null && p.getCategoria().getIdCategoria() != null) {
            categoriaRepo.findById(p.getCategoria().getIdCategoria()).ifPresent(p::setCategoria);
        } else {
            categoriaRepo.findAll().stream().findFirst().ifPresent(p::setCategoria);
        }
        if (p.getEstado() == null)
            p.setEstado(true);
        if (p.getControlaStock() == null)
            p.setControlaStock(false);
        if (p.getStock() == null)
            p.setStock(0);
        if (p.getPermiteCrema() == null)
            p.setPermiteCrema(false);
        if (p.getPermiteSalsa() == null)
            p.setPermiteSalsa(false);
        if (p.getLimiteSalsa() == null)
            p.setLimiteSalsa(0);
        if (p.getTipo() == null)
            p.setTipo(p.inferirTipo());

        return productoRepo.save(p);
    }

    @Transactional
    public Producto actualizar(Integer id, Producto p) {
        Producto existente = productoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));

        existente.setNombre(p.getNombre());
        existente.setDescripcion(p.getDescripcion());
        existente.setPrecio(p.getPrecio());
        existente.setStock(p.getStock());
        existente.setControlaStock(p.getControlaStock());
        if (p.getCategoria() != null && p.getCategoria().getIdCategoria() != null) {
            categoriaRepo.findById(p.getCategoria().getIdCategoria()).ifPresent(existente::setCategoria);
        }
        if (p.getImagen() != null && !p.getImagen().isBlank()) {
            existente.setImagen(p.getImagen());
        }
        existente.setPermiteCrema(p.getPermiteCrema() != null ? p.getPermiteCrema() : false);
        existente.setPermiteSalsa(p.getPermiteSalsa() != null ? p.getPermiteSalsa() : false);
        existente.setLimiteSalsa(p.getLimiteSalsa() != null ? p.getLimiteSalsa() : 0);
        if (p.getTipo() != null) {
            existente.setTipo(p.getTipo());
        } else {
            existente.setTipo(existente.inferirTipo());
        }
        if (p.getEstado() != null) {
            existente.setEstado(p.getEstado());
        }

        return productoRepo.save(existente);
    }

    @Transactional
    public void cambiarEstado(Integer id) {
        productoRepo.findById(id).ifPresent(p -> {
            p.setEstado(p.getEstado() == null || !p.getEstado());
            productoRepo.save(p);
        });
    }

    @Transactional
    public void descontarStock(Integer idProducto, int cantidad) {
        productoRepo.findById(idProducto).ifPresent(p -> {
            if (Boolean.TRUE.equals(p.getControlaStock())) {
                int nuevo = Math.max(0, (p.getStock() != null ? p.getStock() : 0) - cantidad);
                p.setStock(nuevo);
                productoRepo.save(p);
            }
        });
    }

    @Transactional
    public void reponerStock(Integer idProducto, int cantidad) {
        productoRepo.findById(idProducto).ifPresent(p -> {
            if (Boolean.TRUE.equals(p.getControlaStock())) {
                p.setStock((p.getStock() != null ? p.getStock() : 0) + cantidad);
                productoRepo.save(p);
            }
        });
    }
}
