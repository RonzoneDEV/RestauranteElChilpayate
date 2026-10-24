package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Gasto;
import com.cibertec.chilpayate.repository.GastoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class GastoService {

    private final GastoRepository gastoRepo;

    public GastoService(GastoRepository gastoRepo) {
        this.gastoRepo = gastoRepo;
    }

    @Transactional(readOnly = true)
    public List<Gasto> listarTodos() {
        return gastoRepo.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public List<Gasto> listarPorRango(LocalDate desde, LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();
        return gastoRepo.findByFechaBetweenOrderByFechaDesc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public Optional<Gasto> obtenerPorId(Long id) {
        return gastoRepo.findById(id);
    }

    @Transactional
    public Gasto guardar(Gasto g) {
        if (g.getFecha() == null) g.setFecha(LocalDate.now());
        if (g.getConcepto() == null || g.getConcepto().isBlank()) {
            throw new IllegalArgumentException("El concepto del gasto es obligatorio");
        }
        if (g.getMonto() == null || g.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        return gastoRepo.save(g);
    }

    @Transactional
    public Gasto actualizar(Long id, Gasto g) {
        Gasto existente = gastoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado: " + id));

        existente.setFecha(g.getFecha());
        existente.setConcepto(g.getConcepto());
        existente.setMonto(g.getMonto());
        existente.setProveedor(g.getProveedor());
        existente.setObservacion(g.getObservacion());

        return gastoRepo.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        gastoRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalGastosHoy() {
        LocalDate hoy = LocalDate.now();
        return gastoRepo.findByFechaBetweenOrderByFechaDesc(hoy, hoy).stream()
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
