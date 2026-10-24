package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Parámetros de configuración de la Carta para El Chilpayate.
 * Controla límites de extras en salsas para Alitas y límites de adicionales para Platos Principales.
 */
@Entity
@Table(name = "ParametrosCarta")
public class ParametrosCarta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idParametro")
    private Integer idParametro;

    /**
     * Máximo de extras en salsas permitidos por defecto por cada porción de Alitas.
     * Ejemplo: Si es 2, 1 porción de alitas permite hasta 2 extras en salsas, 2 porciones permiten 4, etc.
     */
    @NotNull(message = "El límite de extras en salsas por alitas es obligatorio")
    @Min(value = 0, message = "El límite no puede ser negativo")
    @Column(name = "maxExtrasSalsasPorAlita", nullable = false)
    private Integer maxExtrasSalsasPorAlita = 2;

    /**
     * Máximo de adicionales permitidos por cada Plato Principal (incluye Alitas, Hamburguesas, Salchipapas, etc.).
     * Ejemplo: Si es 2, 1 plato principal permite hasta 2 adicionales, 2 platos permiten 4, etc.
     */
    @NotNull(message = "El límite de adicionales por plato principal es obligatorio")
    @Min(value = 0, message = "El límite no puede ser negativo")
    @Column(name = "maxAdicionalesPorPlatoPrincipal", nullable = false)
    private Integer maxAdicionalesPorPlatoPrincipal = 2;

    @Column(name = "nombreConfiguracion", length = 100)
    private String nombreConfiguracion = "Parámetros Generales de la Carta";

    @Column(name = "descripcion", length = 255)
    private String descripcion = "Control de proporción de extras de salsas por alitas y adicionales por platos principales";

    @Column(name = "fechaActualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Column(name = "modificadoPor", length = 60)
    private String modificadoPor = "admin";

    public ParametrosCarta() {
    }

    public ParametrosCarta(Integer maxExtrasSalsasPorAlita, Integer maxAdicionalesPorPlatoPrincipal) {
        this.maxExtrasSalsasPorAlita = maxExtrasSalsasPorAlita;
        this.maxAdicionalesPorPlatoPrincipal = maxAdicionalesPorPlatoPrincipal;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdParametro() {
        return idParametro;
    }

    public void setIdParametro(Integer idParametro) {
        this.idParametro = idParametro;
    }

    public Integer getMaxExtrasSalsasPorAlita() {
        return maxExtrasSalsasPorAlita != null ? maxExtrasSalsasPorAlita : 2;
    }

    public void setMaxExtrasSalsasPorAlita(Integer maxExtrasSalsasPorAlita) {
        this.maxExtrasSalsasPorAlita = maxExtrasSalsasPorAlita;
    }

    public Integer getMaxAdicionalesPorPlatoPrincipal() {
        return maxAdicionalesPorPlatoPrincipal != null ? maxAdicionalesPorPlatoPrincipal : 2;
    }

    public void setMaxAdicionalesPorPlatoPrincipal(Integer maxAdicionalesPorPlatoPrincipal) {
        this.maxAdicionalesPorPlatoPrincipal = maxAdicionalesPorPlatoPrincipal;
    }

    public String getNombreConfiguracion() {
        return nombreConfiguracion;
    }

    public void setNombreConfiguracion(String nombreConfiguracion) {
        this.nombreConfiguracion = nombreConfiguracion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }
}
