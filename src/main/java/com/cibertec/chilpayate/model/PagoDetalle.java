package com.cibertec.chilpayate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pago_detalle")
public class PagoDetalle {

    @EmbeddedId
    private PagoDetalleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPago")
    @JoinColumn(name = "id_pago", nullable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idDetallePedido")
    @JoinColumn(name = "id_detalle_pedido", nullable = false)
    private DetallePedido detallePedido;

    public PagoDetalle() {
    }

    public PagoDetalleId getId() {
        return id;
    }

    public void setId(PagoDetalleId id) {
        this.id = id;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public DetallePedido getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(DetallePedido detallePedido) {
        this.detallePedido = detallePedido;
    }
}