package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.ParametrosCarta;
import com.cibertec.chilpayate.repository.CategoriaRepository;
import com.cibertec.chilpayate.repository.ParametrosCartaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ParametrosCartaService {

    private final ParametrosCartaRepository parametrosRepo;
    private final CategoriaRepository categoriaRepo;

    public ParametrosCartaService(ParametrosCartaRepository parametrosRepo, CategoriaRepository categoriaRepo) {
        this.parametrosRepo = parametrosRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @Cacheable(value = "parametrosCarta")
    @Transactional
    public ParametrosCarta obtenerParametros() {
        return parametrosRepo.findFirstByOrderByIdParametroAsc().orElseGet(() -> {
            ParametrosCarta inicial = new ParametrosCarta();
            inicial.setMaxExtrasSalsasPorAlita(2);
            inicial.setMaxAdicionalesPorPlatoPrincipal(2);
            inicial.setNombreConfiguracion("Parámetros Generales de la Carta");
            inicial.setDescripcion("Control de proporción de extras de salsas para Alitas y adicionales para Platos Principales");
            inicial.setFechaActualizacion(LocalDateTime.now());
            inicial.setModificadoPor("sistema");
            return parametrosRepo.save(inicial);
        });
    }

    @Transactional(readOnly = true)
    public int getMaxExtrasSalsasPorAlita() {
        return obtenerParametros().getMaxExtrasSalsasPorAlita();
    }

    @Transactional(readOnly = true)
    public int getMaxAdicionalesPorPlatoPrincipal() {
        return obtenerParametros().getMaxAdicionalesPorPlatoPrincipal();
    }

    @CacheEvict(value = "parametrosCarta", allEntries = true)
    @Transactional
    public ParametrosCarta guardarParametros(ParametrosCarta form, String usuario) {
        if (form.getMaxExtrasSalsasPorAlita() == null || form.getMaxExtrasSalsasPorAlita() < 0) {
            throw new IllegalArgumentException("El límite de extras en salsas para Alitas no puede ser negativo");
        }
        if (form.getMaxAdicionalesPorPlatoPrincipal() == null || form.getMaxAdicionalesPorPlatoPrincipal() < 0) {
            throw new IllegalArgumentException("El límite de adicionales para Platos Principales no puede ser negativo");
        }

        ParametrosCarta actual = obtenerParametros();
        actual.setMaxExtrasSalsasPorAlita(form.getMaxExtrasSalsasPorAlita());
        actual.setMaxAdicionalesPorPlatoPrincipal(form.getMaxAdicionalesPorPlatoPrincipal());
        if (form.getDescripcion() != null && !form.getDescripcion().isBlank()) {
            actual.setDescripcion(form.getDescripcion().trim());
        }
        actual.setFechaActualizacion(LocalDateTime.now());
        actual.setModificadoPor(usuario != null && !usuario.isBlank() ? usuario : "admin");

        ParametrosCarta guardado = parametrosRepo.save(actual);

        // Sincronizar también con la categoría Alitas si existe
        categoriaRepo.findByNombreIgnoreCase("Alitas").ifPresent(catAlitas -> {
            catAlitas.setMaxExtras(guardado.getMaxExtrasSalsasPorAlita());
            catAlitas.setMaxAdicionales(guardado.getMaxAdicionalesPorPlatoPrincipal());
            categoriaRepo.save(catAlitas);
        });

        return guardado;
    }
}
