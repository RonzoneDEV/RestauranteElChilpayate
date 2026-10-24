package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.MovimientoStock;
import com.cibertec.chilpayate.model.Producto;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.repository.MovimientoStockRepository;
import com.cibertec.chilpayate.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final MovimientoStockRepository stockRepo;
    private final ProductoRepository productoRepo;

    public StockService(MovimientoStockRepository stockRepo, ProductoRepository productoRepo) {
        this.stockRepo = stockRepo;
        this.productoRepo = productoRepo;
    }

    @Transactional(readOnly = true)
    public List<MovimientoStock> listarMovimientos(Integer idProducto) {
        if (idProducto != null) {
            return stockRepo.findByProducto_IdProductoOrderByFechaHoraDesc(idProducto);
        } else {
            return stockRepo.findTop50ByOrderByFechaHoraDesc();
        }
    }

    @Transactional
    public MovimientoStock registrarMovimiento(Integer idProducto,
                                             MovimientoStock.TipoMovimiento tipoMovimiento,
                                             Integer cantidad,
                                             String motivo,
                                             Usuario usuario) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        Producto prod = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));

        int anterior = prod.getStock() != null ? prod.getStock() : 0;
        int nuevo;

        if (tipoMovimiento == MovimientoStock.TipoMovimiento.ENTRADA) {
            nuevo = anterior + cantidad;
        } else if (tipoMovimiento == MovimientoStock.TipoMovimiento.SALIDA || tipoMovimiento == MovimientoStock.TipoMovimiento.MERMA) {
            nuevo = Math.max(0, anterior - cantidad);
        } else { // AJUSTE
            nuevo = cantidad;
        }

        prod.setStock(nuevo);
        productoRepo.save(prod);

        MovimientoStock mov = new MovimientoStock();
        mov.setProducto(prod);
        mov.setTipoMovimiento(tipoMovimiento);
        mov.setCantidad(cantidad);
        mov.setStockAnterior(anterior);
        mov.setStockNuevo(nuevo);
        mov.setMotivo(motivo);
        mov.setFechaHora(LocalDateTime.now());
        mov.setUsuario(usuario);

        return stockRepo.save(mov);
    }

    @Transactional
    public MovimientoStock ajustarStockRapido(Integer idProducto, int delta, Usuario usuario) {
        Producto prod = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));

        int anterior = prod.getStock() != null ? prod.getStock() : 0;
        int nuevo = Math.max(0, anterior + delta);
        prod.setStock(nuevo);
        productoRepo.save(prod);

        MovimientoStock mov = new MovimientoStock();
        mov.setProducto(prod);
        mov.setTipoMovimiento(delta >= 0 ? MovimientoStock.TipoMovimiento.ENTRADA : MovimientoStock.TipoMovimiento.SALIDA);
        mov.setCantidad(Math.max(1, Math.abs(delta)));
        mov.setStockAnterior(anterior);
        mov.setStockNuevo(nuevo);
        mov.setMotivo(delta >= 0
                ? "Ajuste rápido carta (+ " + delta + " uds)"
                : "Ajuste rápido carta (- " + Math.abs(delta) + " uds)");
        mov.setFechaHora(LocalDateTime.now());
        mov.setUsuario(usuario);

        return stockRepo.save(mov);
    }

    @Transactional
    public MovimientoStock fijarStockDirecto(Integer idProducto, int nuevoStock, Usuario usuario, String motivo) {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }

        Producto prod = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));

        int anterior = prod.getStock() != null ? prod.getStock() : 0;
        int diff = Math.abs(nuevoStock - anterior);
        prod.setStock(nuevoStock);
        productoRepo.save(prod);

        MovimientoStock mov = new MovimientoStock();
        mov.setProducto(prod);
        mov.setTipoMovimiento(MovimientoStock.TipoMovimiento.AJUSTE);
        mov.setCantidad(Math.max(1, diff));
        mov.setStockAnterior(anterior);
        mov.setStockNuevo(nuevoStock);
        mov.setMotivo(motivo != null && !motivo.isBlank()
                ? motivo
                : (nuevoStock == 0 ? "Marcado como AGOTADO en carta" : "Ajuste directo en carta a " + nuevoStock + " uds"));
        mov.setFechaHora(LocalDateTime.now());
        mov.setUsuario(usuario);

        return stockRepo.save(mov);
    }

    @Transactional
    public int aperturaTurnoPorCategoria(Integer idCategoria, String categoriaNombre, int cantidadBase, Usuario usuario) {
        if (cantidadBase < 0) {
            throw new IllegalArgumentException("La cantidad base no puede ser negativa");
        }

        List<Producto> productos;
        if (idCategoria != null && idCategoria > 0) {
            productos = productoRepo.findByCategoria_IdCategoriaAndEstadoTrue(idCategoria);
        } else if (categoriaNombre != null && !categoriaNombre.isBlank() && !categoriaNombre.equalsIgnoreCase("TODOS")) {
            productos = productoRepo.findByEstadoTrue().stream()
                    .filter(p -> p.getCategoria() != null &&
                            (p.getCategoria().getNombre().equalsIgnoreCase(categoriaNombre) ||
                             p.getCategoria().getNombre().toLowerCase().contains(categoriaNombre.toLowerCase())))
                    .toList();
        } else {
            productos = productoRepo.findByEstadoTrue();
        }

        int count = 0;
        for (Producto prod : productos) {
            int anterior = prod.getStock() != null ? prod.getStock() : 0;
            if (anterior == 0) {
                prod.setStock(cantidadBase);
                productoRepo.save(prod);

                MovimientoStock mov = new MovimientoStock();
                mov.setProducto(prod);
                mov.setTipoMovimiento(MovimientoStock.TipoMovimiento.ENTRADA);
                mov.setCantidad(cantidadBase);
                mov.setStockAnterior(0);
                mov.setStockNuevo(cantidadBase);
                mov.setMotivo("Apertura de turno: Stock inicial " + cantidadBase + " uds");
                mov.setFechaHora(LocalDateTime.now());
                mov.setUsuario(usuario);
                stockRepo.save(mov);

                count++;
            }
        }
        return count;
    }
}
