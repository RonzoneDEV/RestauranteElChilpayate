package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPedido")
    private Integer idPedido;

    @Column(name = "codigo", length = 20, unique = true)
    private String codigo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoPedido tipo = TipoPedido.SALON;

    @ManyToOne
    @JoinColumn(name = "idMesa", nullable = true)
    private Mesa mesa;

    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = true)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = true)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPedido estado = EstadoPedido.BORRADOR;

    @Column(name = "HoraFinal")
    private LocalDateTime horaFinal;

    @OneToMany(mappedBy = "pedido")
    private List<TicketCocina> ticketCocina;

    @OneToMany(mappedBy = "pedido")
    private List<DetallePedido> detallePedido;

    @OneToOne(mappedBy = "pedido")
    private PedidoDelivery pedidoDelivery;

    @OneToMany(mappedBy = "pedido")
    private List<Venta> ventas;

    @OneToMany(mappedBy = "pedido")
    private List<Pago> pagos;

    @OneToMany(mappedBy = "pedido")
    private List<Precuenta> precuentas;

    @OneToMany(mappedBy = "pedido")
    private List<Incidente> incidentes;

    public Pedido() {
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public TipoPedido getTipo() {
        return tipo;
    }

    public void setTipo(TipoPedido tipo) {
        this.tipo = tipo;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public LocalDateTime getHoraFinal() {
        return horaFinal;
    }

    public void setHoraFinal(LocalDateTime horaFinal) {
        this.horaFinal = horaFinal;
    }

    public enum TipoPedido {
        SALON, DELIVERY
    }

    public enum EstadoPedido {
        BORRADOR, ACTIVA, PRECUENTA, PAGADA, ANULADA
    }

    public List<TicketCocina> getTicketCocina() {
        return ticketCocina;
    }

    public void setTicketCocina(List<TicketCocina> ticketCocina) {
        this.ticketCocina = ticketCocina;
    }

    public List<DetallePedido> getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(List<DetallePedido> detallePedido) {
        this.detallePedido = detallePedido;
    }

    public PedidoDelivery getPedidoDelivery() {
        return pedidoDelivery;
    }

    public void setPedidoDelivery(PedidoDelivery pedidoDelivery) {
        this.pedidoDelivery = pedidoDelivery;
    }

    public List<Venta> getVentas() {
        return ventas;
    }

    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }

    public List<Precuenta> getPrecuentas() {
        return precuentas;
    }

    public void setPrecuentas(List<Precuenta> precuentas) {
        this.precuentas = precuentas;
    }

    public List<Incidente> getIncidentes() {
        return incidentes;
    }

    public void setIncidentes(List<Incidente> incidentes) {
        this.incidentes = incidentes;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    @Transient
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (detallePedido != null) {
            for (DetallePedido d : detallePedido) {
                if (d != null && d.getEstado() != DetallePedido.EstadoDetalle.ANULADO && d.getPrecioActual() != null) {
                    total = total.add(d.getPrecioActual());
                }
            }
        }
        if (pedidoDelivery != null && pedidoDelivery.getCostoDelivery() != null) {
            total = total.add(pedidoDelivery.getCostoDelivery());
        }
        return total;
    }
}