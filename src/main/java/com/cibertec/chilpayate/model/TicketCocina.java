package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "TicketCocina")
public class TicketCocina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTicket")
    private Integer idTicket;

    @ManyToOne
    @JoinColumn(name = "idPedido", nullable = false)
    private Pedido pedido;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo = 1;

    @Column(name = "fechaHora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTicket estado = EstadoTicket.IMPRESO;
    
    @OneToMany(mappedBy = "ticketCocina")
    private List<DetallePedido> detallePedido;

    public TicketCocina() {}

    public Integer getIdTicket() { return idTicket; }
    public void setIdTicket(Integer idTicket) { this.idTicket = idTicket; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Integer getCorrelativo() { return correlativo; }
    public void setCorrelativo(Integer correlativo) { this.correlativo = correlativo; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }

    public enum EstadoTicket {
        IMPRESO, PREPARANDO, LISTO, ANULADO
    }

    public List<DetallePedido> getDetallePedido() { return detallePedido; }
    public void setDetallePedido(List<DetallePedido> detallePedido) { this.detallePedido = detallePedido; }

    public boolean isTodosListos() {
        if (detallePedido == null || detallePedido.isEmpty()) {
            return false;
        }
        return detallePedido.stream()
                .filter(d -> d.getEstado() != DetallePedido.EstadoDetalle.ANULADO)
                .allMatch(d -> d.getEstado() == DetallePedido.EstadoDetalle.LISTO || d.getEstado() == DetallePedido.EstadoDetalle.ENTREGADO);
    }

    public boolean getTodosListos() {
        return isTodosListos();
    }

    public boolean isPuedeCompletar() {
        return estado == EstadoTicket.PREPARANDO && isTodosListos();
    }

    public boolean getPuedeCompletar() {
        return isPuedeCompletar();
    }
}