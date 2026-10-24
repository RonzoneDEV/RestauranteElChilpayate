package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Precuenta")
public class Precuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPrecuenta")
    private Integer idPrecuenta;

    @ManyToOne
    @JoinColumn(name = "idPedido", nullable = false)
    private Pedido pedido;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPago", nullable = true)
    private Pago pago;

    @Column(name = "numeroPrecuenta", nullable = false)
    private Integer numeroPrecuenta = 1;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "igv", nullable = false, precision = 10, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "fechaHora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPrecuenta estado = EstadoPrecuenta.PENDIENTE;

    @OneToMany(mappedBy = "precuenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrecuentaDetalle> detalles = new ArrayList<>();

    public enum EstadoPrecuenta {
        PENDIENTE, PAGADA, ANULADA
    }

    public Precuenta() {
    }

    public Integer getIdPrecuenta() {
        return idPrecuenta;
    }

    public void setIdPrecuenta(Integer idPrecuenta) {
        this.idPrecuenta = idPrecuenta;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public Integer getNumeroPrecuenta() {
        return numeroPrecuenta;
    }

    public void setNumeroPrecuenta(Integer numeroPrecuenta) {
        this.numeroPrecuenta = numeroPrecuenta;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public EstadoPrecuenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrecuenta estado) {
        this.estado = estado;
    }

    public List<PrecuentaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<PrecuentaDetalle> detalles) {
        this.detalles = detalles;
    }
}
