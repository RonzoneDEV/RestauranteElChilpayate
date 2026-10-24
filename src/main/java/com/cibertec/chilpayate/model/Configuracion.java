package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "Configuracion")
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idConfiguracion")
    private Integer idConfiguracion;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "ruc", nullable = false, length = 11)
    private String ruc;

    @Column(name = "direccion", nullable = false, length = 150)
    private String direccion;

    @Column(name = "urbanizacion", length = 100)
    private String urbanizacion;

    @Column(name = "departamento", length = 50)
    private String departamento;

    @Column(name = "provincia", length = 50)
    private String provincia;

    @Column(name = "distrito", length = 50)
    private String distrito;

    @Column(name = "inicioTurno", nullable = false)
    @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
    private LocalTime inicioTurno;

    @Column(name = "finTurno", nullable = false)
    @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
    private LocalTime finTurno;

    public Configuracion() {}

    public Integer getIdConfiguracion() { return idConfiguracion; }
    public void setIdConfiguracion(Integer idConfiguracion) { this.idConfiguracion = idConfiguracion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getUrbanizacion() { return urbanizacion; }
    public void setUrbanizacion(String urbanizacion) { this.urbanizacion = urbanizacion; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public LocalTime getInicioTurno() { return inicioTurno; }
    public void setInicioTurno(LocalTime inicioTurno) { this.inicioTurno = inicioTurno; }

    public LocalTime getFinTurno() { return finTurno; }
    public void setFinTurno(LocalTime finTurno) { this.finTurno = finTurno; }

    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();
        if (direccion != null && !direccion.isBlank()) sb.append(direccion.trim());
        if (urbanizacion != null && !urbanizacion.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(urbanizacion.trim());
        }
        if (distrito != null && !distrito.isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(distrito.trim());
        }
        if (provincia != null && !provincia.isBlank() && !provincia.equalsIgnoreCase(distrito)) {
            sb.append(" - ").append(provincia.trim());
        }
        if (departamento != null && !departamento.isBlank()) {
            sb.append(" - ").append(departamento.trim());
        }
        return sb.length() > 0 ? sb.toString() : "Carlos Wiesse 625, Santo Dominguito · Trujillo - La Libertad";
    }

    public String getHorarioTurno() {
        if (inicioTurno == null || finTurno == null) return "6:00 PM - 11:30 PM";
        return inicioTurno.toString() + " a " + finTurno.toString();
    }
}