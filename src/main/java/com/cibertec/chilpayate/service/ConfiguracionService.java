package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Configuracion;
import com.cibertec.chilpayate.repository.ConfiguracionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
public class ConfiguracionService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionService.class);

    public static final String DEFAULT_NOMBRE = "El Chilpayate";
    public static final String DEFAULT_RUC = "10876343874";
    public static final String DEFAULT_DIRECCION = "Carlos Wiesse 625";
    public static final String DEFAULT_URBANIZACION = "Santo Dominguito";
    public static final String DEFAULT_DEPARTAMENTO = "La Libertad";
    public static final String DEFAULT_PROVINCIA = "Trujillo";
    public static final String DEFAULT_DISTRITO = "Trujillo";
    public static final LocalTime DEFAULT_INICIO_TURNO = LocalTime.of(18, 0); // 6:00 pm
    public static final LocalTime DEFAULT_FIN_TURNO = LocalTime.of(23, 30);   // 11:30 pm

    private final ConfiguracionRepository configuracionRepo;

    public ConfiguracionService(ConfiguracionRepository configuracionRepo) {
        this.configuracionRepo = configuracionRepo;
    }

    @Transactional
    public Configuracion obtenerConfiguracion() {
        return configuracionRepo.findTopByOrderByIdConfiguracionAsc()
                .orElseGet(this::crearConfiguracionPorDefecto);
    }

    @Transactional
    public Configuracion crearConfiguracionPorDefecto() {
        log.info("Creando configuración inicial por defecto de la empresa: {}", DEFAULT_NOMBRE);
        Configuracion c = new Configuracion();
        c.setNombre(DEFAULT_NOMBRE);
        c.setRuc(DEFAULT_RUC);
        c.setDireccion(DEFAULT_DIRECCION);
        c.setUrbanizacion(DEFAULT_URBANIZACION);
        c.setDepartamento(DEFAULT_DEPARTAMENTO);
        c.setProvincia(DEFAULT_PROVINCIA);
        c.setDistrito(DEFAULT_DISTRITO);
        c.setInicioTurno(DEFAULT_INICIO_TURNO);
        c.setFinTurno(DEFAULT_FIN_TURNO);
        return configuracionRepo.save(c);
    }

    @Transactional
    public Configuracion guardar(Configuracion datos) {
        Configuracion actual = configuracionRepo.findTopByOrderByIdConfiguracionAsc()
                .orElseGet(Configuracion::new);

        actual.setNombre(datos.getNombre() != null && !datos.getNombre().isBlank() ? datos.getNombre().trim() : DEFAULT_NOMBRE);
        actual.setRuc(datos.getRuc() != null && !datos.getRuc().isBlank() ? datos.getRuc().trim() : DEFAULT_RUC);
        actual.setDireccion(datos.getDireccion() != null && !datos.getDireccion().isBlank() ? datos.getDireccion().trim() : DEFAULT_DIRECCION);
        actual.setUrbanizacion(datos.getUrbanizacion() != null ? datos.getUrbanizacion().trim() : DEFAULT_URBANIZACION);
        actual.setDepartamento(datos.getDepartamento() != null && !datos.getDepartamento().isBlank() ? datos.getDepartamento().trim() : DEFAULT_DEPARTAMENTO);
        actual.setProvincia(datos.getProvincia() != null && !datos.getProvincia().isBlank() ? datos.getProvincia().trim() : DEFAULT_PROVINCIA);
        actual.setDistrito(datos.getDistrito() != null && !datos.getDistrito().isBlank() ? datos.getDistrito().trim() : DEFAULT_DISTRITO);
        actual.setInicioTurno(datos.getInicioTurno() != null ? datos.getInicioTurno() : DEFAULT_INICIO_TURNO);
        actual.setFinTurno(datos.getFinTurno() != null ? datos.getFinTurno() : DEFAULT_FIN_TURNO);

        log.info("Configuración de empresa actualizada: [{} - RUC: {}]", actual.getNombre(), actual.getRuc());
        return configuracionRepo.save(actual);
    }
}
