package com.cibertec.chilpayate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "DetallePedidoSalsa")
public class DetallePedidoSalsa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetalleSalsa")
    private Integer idDetalleSalsa;

    @ManyToOne
    @JoinColumn(name = "idDetallePedido", nullable = false)
    private DetallePedido detallePedido;

    @ManyToOne
    @JoinColumn(name = "idSalsa", nullable = false)
    private Producto salsa;

    public DetallePedidoSalsa() {}

    public Integer getIdDetalleSalsa() { return idDetalleSalsa; }
    public void setIdDetalleSalsa(Integer idDetalleSalsa) { this.idDetalleSalsa = idDetalleSalsa; }


    public Producto getSalsa() { return salsa; }
    public void setSalsa(Producto salsa) { this.salsa = salsa; }

    public DetallePedido getDetallePedido() { return detallePedido; }
    public void setDetallePedido(DetallePedido detallePedido) { this.detallePedido = detallePedido; }
}