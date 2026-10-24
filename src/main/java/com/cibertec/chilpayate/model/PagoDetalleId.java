package com.cibertec.chilpayate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PagoDetalleId implements Serializable {

    @Column(name = "id_pago")
    private Integer idPago;

    @Column(name = "id_detalle_pedido")
    private Integer idDetallePedido;

    public PagoDetalleId() {}

    public PagoDetalleId(Integer idPago, Integer idDetallePedido) {
        this.idPago = idPago;
        this.idDetallePedido = idDetallePedido;
    }

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }

    public Integer getIdDetallePedido() { return idDetallePedido; }
    public void setIdDetallePedido(Integer idDetallePedido) { this.idDetallePedido = idDetallePedido; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagoDetalleId that = (PagoDetalleId) o;
        return Objects.equals(idPago, that.idPago) &&
               Objects.equals(idDetallePedido, that.idDetallePedido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPago, idDetallePedido);
    }
}