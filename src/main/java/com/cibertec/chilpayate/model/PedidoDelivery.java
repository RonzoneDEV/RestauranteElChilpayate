package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PedidoDelivery")
public class PedidoDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDelivery")
    private Integer idDelivery;

    @OneToOne
    @JoinColumn(name = "idPedido", nullable = false)
    private Pedido pedido;

    @Column(name = "direccion", nullable = false, length = 150)
    private String direccion;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "Referencia", nullable = false, length = 200)
    private String referencia;

    @Column(name = "CostoDelivery", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoDelivery = BigDecimal.ZERO;

    @Column(name = "fechaAsignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fechaEntrega")
    private LocalDateTime fechaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoDelivery estado;

    @Column(name = "repartidor", length = 100)
    private String repartidor;

    public PedidoDelivery() {}

    public Integer getIdDelivery() { return idDelivery; }
    public void setIdDelivery(Integer idDelivery) { this.idDelivery = idDelivery; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public BigDecimal getCostoDelivery() { return costoDelivery; }
    public void setCostoDelivery(BigDecimal costoDelivery) { this.costoDelivery = costoDelivery; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public EstadoDelivery getEstado() { return estado; }
    public void setEstado(EstadoDelivery estado) { this.estado = estado; }

    public String getRepartidor() { return repartidor; }
    public void setRepartidor(String repartidor) { this.repartidor = repartidor; }

    @Transient
    public Boolean getPagado() {
        if (pedido == null) return false;
        return (pedido.getPagos() != null && !pedido.getPagos().isEmpty())
                || pedido.getEstado() == Pedido.EstadoPedido.PAGADA;
    }

    @Transient
    public boolean isPagado() {
        return Boolean.TRUE.equals(getPagado());
    }

    @Transient
    public String getMetodoPago() {
        if (pedido != null && pedido.getPagos() != null && !pedido.getPagos().isEmpty()) {
            Pago p = pedido.getPagos().get(0);
            if (p.getPagoMedio() != null && !p.getPagoMedio().isEmpty()) {
                PagoMedio pm = p.getPagoMedio().get(0);
                if (pm.getMetodoPago() != null) {
                    return pm.getMetodoPago().getNombre();
                }
            }
        }
        return "Por pagar";
    }

    @Transient
    public String getNumeroOperacion() {
        if (pedido != null && pedido.getPagos() != null && !pedido.getPagos().isEmpty()) {
            Pago p = pedido.getPagos().get(0);
            if (p.getPagoMedio() != null && !p.getPagoMedio().isEmpty()) {
                String op = p.getPagoMedio().get(0).getNumeroOperacion();
                return op != null ? op : "";
            }
        }
        return "";
    }

    @Transient
    public Venta getVenta() {
        if (pedido != null && pedido.getVentas() != null && !pedido.getVentas().isEmpty()) {
            return pedido.getVentas().get(0);
        }
        return null;
    }

    @Transient
    public Pago getPago() {
        if (pedido != null && pedido.getPagos() != null && !pedido.getPagos().isEmpty()) {
            return pedido.getPagos().get(0);
        }
        return null;
    }

    public enum EstadoDelivery {
        PENDIENTE_PAGO,
        EN_PREPARACION,
        LISTO,
        EN_CAMINO,
        ENTREGADO,
        ANULADO
    }
}