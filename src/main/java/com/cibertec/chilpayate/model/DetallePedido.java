package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "DetallePedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetallePedido")
    private Integer idDetallePedido;

    @ManyToOne
    @JoinColumn(name = "idPedido", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "idTicket")
    private TicketCocina ticketCocina;

    @ManyToOne
    @JoinColumn(name = "idProducto", nullable = false)
    private Producto producto;

    @Column(name = "nota", length = 255)
    private String nota;

    @Column(name = "precioActual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoDetalle estado = EstadoDetalle.BORRADOR;

    @OneToMany(mappedBy = "detallePedido")
    private List<DetallePedidoCrema> detallePedidoCrema;

    @OneToMany(mappedBy = "detallePedido")
    private List<DetallePedidoSalsa> detallePedidoSalsa;

    @OneToMany(mappedBy = "detallePedido")
    private List<PagoDetalle> pagoDetalle;

    @OneToMany(mappedBy = "detallePedido")
    private List<PrecuentaDetalle> precuentaDetalle;

    @OneToMany(mappedBy = "detallePedido")
    private List<Incidente> incidentes;

    public DetallePedido() {
    }

    public Integer getIdDetallePedido() {
        return idDetallePedido;
    }

    public void setIdDetallePedido(Integer idDetallePedido) {
        this.idDetallePedido = idDetallePedido;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public TicketCocina getTicketCocina() {
        return ticketCocina;
    }

    public void setTicketCocina(TicketCocina ticketCocina) {
        this.ticketCocina = ticketCocina;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public BigDecimal getPrecioActual() {
        return precioActual;
    }

    public void setPrecioActual(BigDecimal precioActual) {
        this.precioActual = precioActual;
    }

    public EstadoDetalle getEstado() {
        return estado;
    }

    public void setEstado(EstadoDetalle estado) {
        this.estado = estado;
    }

    public enum EstadoDetalle {
        BORRADOR, EN_COCINA, LISTO, ENTREGADO, ANULADO
    }

    public List<DetallePedidoCrema> getDetallePedidoCrema() {
        return detallePedidoCrema;
    }

    public void setDetallePedidoCrema(List<DetallePedidoCrema> detallePedidoCrema) {
        this.detallePedidoCrema = detallePedidoCrema;
    }

    public List<DetallePedidoSalsa> getDetallePedidoSalsa() {
        return detallePedidoSalsa;
    }

    public void setDetallePedidoSalsa(List<DetallePedidoSalsa> detallePedidoSalsa) {
        this.detallePedidoSalsa = detallePedidoSalsa;
    }

    public List<PagoDetalle> getPagoDetalle() {
        return pagoDetalle;
    }

    public void setPagoDetalle(List<PagoDetalle> pagoDetalle) {
        this.pagoDetalle = pagoDetalle;
    }

    public List<Incidente> getIncidentes() {
        return incidentes;
    }

    public void setIncidentes(List<Incidente> incidentes) {
        this.incidentes = incidentes;
    }

    public List<PrecuentaDetalle> getPrecuentaDetalle() {
        return precuentaDetalle;
    }

    public void setPrecuentaDetalle(List<PrecuentaDetalle> precuentaDetalle) {
        this.precuentaDetalle = precuentaDetalle;
    }
}