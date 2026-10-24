package com.cibertec.chilpayate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "DetallePedidoCrema")
public class DetallePedidoCrema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetalleCrema")
    private Integer idDetalleCrema;

    @ManyToOne
    @JoinColumn(name = "idDetallePedido", nullable = false)
    private DetallePedido detallePedido;

    @ManyToOne
    @JoinColumn(name = "idCrema", nullable = false)
    private Crema crema;

    public DetallePedidoCrema() {}

    public Integer getIdDetalleCrema() { return idDetalleCrema; }
    public void setIdDetalleCrema(Integer idDetalleCrema) { this.idDetalleCrema = idDetalleCrema; }

    public Crema getCrema() { return crema; }
    public void setCrema(Crema crema) { this.crema = crema; }

    public DetallePedido getDetallePedido() { return detallePedido; }
    public void setDetallePedido(DetallePedido detallePedido) { this.detallePedido = detallePedido; }
}