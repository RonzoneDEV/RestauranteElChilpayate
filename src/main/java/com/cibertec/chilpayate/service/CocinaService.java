package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.DetallePedido;
import com.cibertec.chilpayate.model.PedidoDelivery;
import com.cibertec.chilpayate.model.TicketCocina;
import com.cibertec.chilpayate.repository.DetallePedidoRepository;
import com.cibertec.chilpayate.repository.PedidoDeliveryRepository;
import com.cibertec.chilpayate.repository.TicketCocinaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CocinaService {

    private final TicketCocinaRepository ticketRepo;
    private final DetallePedidoRepository detalleRepo;
    private final PedidoDeliveryRepository deliveryRepo;

    public CocinaService(TicketCocinaRepository ticketRepo,
                         DetallePedidoRepository detalleRepo,
                         PedidoDeliveryRepository deliveryRepo) {
        this.ticketRepo = ticketRepo;
        this.detalleRepo = detalleRepo;
        this.deliveryRepo = deliveryRepo;
    }

    @Transactional(readOnly = true)
    public List<TicketCocina> listarTicketsActivos() {
        return ticketRepo.findByEstadoInOrderByFechaHoraAsc(
                List.of(TicketCocina.EstadoTicket.IMPRESO, TicketCocina.EstadoTicket.PREPARANDO)
        );
    }

    @Transactional
    public Optional<TicketCocina> marcarPreparando(Integer idTicket) {
        return ticketRepo.findById(idTicket).map(t -> {
            t.setEstado(TicketCocina.EstadoTicket.PREPARANDO);
            ticketRepo.save(t);
            return t;
        });
    }

    @Transactional
    public Optional<TicketCocina> marcarListo(Integer idTicket) {
        return ticketRepo.findById(idTicket).map(t -> {
            t.setEstado(TicketCocina.EstadoTicket.LISTO);
            ticketRepo.save(t);

            if (t.getDetallePedido() != null) {
                for (DetallePedido d : t.getDetallePedido()) {
                    if (d.getEstado() == DetallePedido.EstadoDetalle.EN_COCINA || d.getEstado() == DetallePedido.EstadoDetalle.BORRADOR) {
                        d.setEstado(DetallePedido.EstadoDetalle.LISTO);
                        detalleRepo.save(d);
                    }
                }
            }

            if (t.getPedido() != null && t.getPedido().getPedidoDelivery() != null) {
                PedidoDelivery del = t.getPedido().getPedidoDelivery();
                if (del.getEstado() == PedidoDelivery.EstadoDelivery.EN_PREPARACION) {
                    del.setEstado(PedidoDelivery.EstadoDelivery.LISTO);
                    deliveryRepo.save(del);
                }
            }

            return t;
        });
    }

    @Transactional
    public boolean toggleItemListo(Integer idDetalle) {
        Optional<DetallePedido> opt = detalleRepo.findById(idDetalle);
        if (opt.isEmpty()) return false;

        DetallePedido d = opt.get();
        if (d.getTicketCocina() != null && d.getTicketCocina().getEstado() == TicketCocina.EstadoTicket.IMPRESO) {
            throw new IllegalStateException("Debes hacer clic en 'Iniciar Pedido' antes de marcar los platos.");
        }

        if (d.getEstado() == DetallePedido.EstadoDetalle.LISTO) {
            d.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);
        } else {
            d.setEstado(DetallePedido.EstadoDetalle.LISTO);
        }
        detalleRepo.save(d);
        return true;
    }

    @Transactional
    public Map<String, Object> toggleItemListoAjax(Integer idDetalle) {
        DetallePedido d = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado con ID: " + idDetalle));

        if (d.getTicketCocina() != null && d.getTicketCocina().getEstado() == TicketCocina.EstadoTicket.IMPRESO) {
            return Map.of("success", false, "error", "Debes hacer clic en 'Iniciar Pedido' antes de marcar los platos.");
        }

        DetallePedido.EstadoDetalle nuevo;
        if (d.getEstado() == DetallePedido.EstadoDetalle.LISTO) {
            nuevo = DetallePedido.EstadoDetalle.EN_COCINA;
        } else {
            nuevo = DetallePedido.EstadoDetalle.LISTO;
        }
        d.setEstado(nuevo);
        detalleRepo.save(d);

        boolean todosListos = false;
        Integer idTicket = 0;
        if (d.getTicketCocina() != null) {
            TicketCocina t = d.getTicketCocina();
            final Integer targetTicketId = t.getIdTicket();
            idTicket = targetTicketId;
            List<DetallePedido> detalles = detalleRepo.findByTicketCocina_IdTicket(targetTicketId);
            if (detalles.isEmpty() && t.getPedido() != null) {
                detalles = detalleRepo.findByPedido_IdPedido(t.getPedido().getIdPedido()).stream()
                        .filter(item -> item.getTicketCocina() != null && targetTicketId.equals(item.getTicketCocina().getIdTicket()))
                        .toList();
            }
            todosListos = !detalles.isEmpty() && detalles.stream()
                    .filter(item -> item.getEstado() != DetallePedido.EstadoDetalle.ANULADO)
                    .allMatch(item -> item.getEstado() == DetallePedido.EstadoDetalle.LISTO || item.getEstado() == DetallePedido.EstadoDetalle.ENTREGADO);
        }

        return Map.of(
                "success", true,
                "idDetalle", idDetalle,
                "idTicket", idTicket != null ? idTicket : 0,
                "nuevoEstado", nuevo.name(),
                "esListo", nuevo == DetallePedido.EstadoDetalle.LISTO,
                "todosListos", todosListos
        );
    }
}
