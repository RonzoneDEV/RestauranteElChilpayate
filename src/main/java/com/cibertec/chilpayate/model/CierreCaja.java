package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CierreCaja")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCaja")
    private Integer idCaja;

    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fechaApertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fechaCierre")
    private LocalDateTime fechaCierre;

    @Column(name = "montoApertura", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoApertura = BigDecimal.ZERO;

    @Column(name = "totalVenta", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVenta;

    @Column(name = "totalGasto", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalGasto;

    @Column(name = "diferencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal diferencia;

    public CierreCaja() {}

    public Integer getIdCaja() { return idCaja; }
    public void setIdCaja(Integer idCaja) { this.idCaja = idCaja; }

    // Alias para compatibilidad
    public Integer getIdCierre() { return idCaja; }
    public void setIdCierre(Integer idCierre) { this.idCaja = idCierre; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public BigDecimal getMontoApertura() { return montoApertura; }
    public void setMontoApertura(BigDecimal montoApertura) { this.montoApertura = montoApertura; }

    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }

    public BigDecimal getTotalGasto() { return totalGasto; }
    public void setTotalGasto(BigDecimal totalGasto) { this.totalGasto = totalGasto; }

    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }
}