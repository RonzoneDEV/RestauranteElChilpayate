package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PagoMedio")
public class PagoMedio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPagoMedio")
    private Integer idPagoMedio;

    @ManyToOne
    @JoinColumn(name = "idPago", nullable = false)
    private Pago pago;

    @ManyToOne
    @JoinColumn(name = "idMetodoPago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "numeroOperacion", length = 80)
    private String numeroOperacion;

    public PagoMedio() {}

    public Integer getIdPagoMedio() { return idPagoMedio; }
    public void setIdPagoMedio(Integer idPagoMedio) { this.idPagoMedio = idPagoMedio; }

    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }
}