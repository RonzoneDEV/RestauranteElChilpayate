package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago")
    private Integer idPago;

    @ManyToOne
    @JoinColumn(name = "idPedido", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoPago", nullable = false)
    private TipoPago tipoPago;

    @Column(name = "codigoDocumento", nullable = false, length = 100, unique = true)
    private String codigoDocumento;

    @Column(name = "montoTotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "fechaHora", nullable = false)
    private LocalDateTime fechaHora;
    
    @OneToMany(mappedBy = "pago")
    private List<PagoDetalle> pagoDetalle;
    
    @OneToMany(mappedBy = "pago")
    private List<PagoMedio> pagoMedio;

    @OneToOne(mappedBy = "pago", fetch = FetchType.LAZY)
    private Venta venta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idPrecuenta", nullable = true)
    private Precuenta precuenta;

    public Pago() {}

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public TipoPago getTipoPago() { return tipoPago; }
    public void setTipoPago(TipoPago tipoPago) { this.tipoPago = tipoPago; }

    public String getCodigoDocumento() { return codigoDocumento; }
    public void setCodigoDocumento(String codigoDocumento) { this.codigoDocumento = codigoDocumento; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public enum TipoPago { UNICO, SPLIT }

	public List<PagoDetalle> getPagoDetalle() { return pagoDetalle; }
	public void setPagoDetalle(List<PagoDetalle> pagoDetalle) { this.pagoDetalle = pagoDetalle; }

	public List<PagoMedio> getPagoMedio() { return pagoMedio; }
	public void setPagoMedio(List<PagoMedio> pagoMedio) { this.pagoMedio = pagoMedio; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
	public List<Venta> getVentas() { return venta != null ? List.of(venta) : List.of(); }
	public void setVentas(List<Venta> ventas) { if (ventas != null && !ventas.isEmpty()) this.venta = ventas.get(0); }

    public Precuenta getPrecuenta() { return precuenta; }
    public void setPrecuenta(Precuenta precuenta) { this.precuenta = precuenta; }
    public List<Precuenta> getPrecuentas() { return precuenta != null ? List.of(precuenta) : List.of(); }
    public void setPrecuentas(List<Precuenta> precuentas) { if (precuentas != null && !precuentas.isEmpty()) this.precuenta = precuentas.get(0); }
}