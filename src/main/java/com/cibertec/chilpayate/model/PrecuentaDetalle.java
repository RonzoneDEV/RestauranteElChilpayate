package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PrecuentaDetalle")
public class PrecuentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPrecuentaDetalle")
    private Integer idPrecuentaDetalle;

    @ManyToOne
    @JoinColumn(name = "idPrecuenta", nullable = false)
    private Precuenta precuenta;

    @ManyToOne
    @JoinColumn(name = "idDetallePedido", nullable = false)
    private DetallePedido detallePedido;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    public PrecuentaDetalle() {}

    public PrecuentaDetalle(Precuenta precuenta, DetallePedido detallePedido, BigDecimal precio) {
        this.precuenta = precuenta;
        this.detallePedido = detallePedido;
        this.precio = precio;
    }

    public Integer getIdPrecuentaDetalle() {
        return idPrecuentaDetalle;
    }

    public void setIdPrecuentaDetalle(Integer idPrecuentaDetalle) {
        this.idPrecuentaDetalle = idPrecuentaDetalle;
    }

    public Precuenta getPrecuenta() {
        return precuenta;
    }

    public void setPrecuenta(Precuenta precuenta) {
        this.precuenta = precuenta;
    }

    public DetallePedido getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(DetallePedido detallePedido) {
        this.detallePedido = detallePedido;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
