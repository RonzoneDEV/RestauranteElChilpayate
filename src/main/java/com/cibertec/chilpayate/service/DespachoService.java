package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.controllers.DespachoController.DespachoPedidoDTO;
import com.cibertec.chilpayate.model.DetallePedido;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.TicketCocina;
import com.cibertec.chilpayate.repository.DetallePedidoRepository;
import com.cibertec.chilpayate.repository.PedidoRepository;
import com.cibertec.chilpayate.repository.TicketCocinaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DespachoService {

    private final TicketCocinaRepository ticketRepo;
    private final DetallePedidoRepository detalleRepo;
    private final PedidoRepository pedidoRepo;

    public DespachoService(TicketCocinaRepository ticketRepo,
                           DetallePedidoRepository detalleRepo,
                           PedidoRepository pedidoRepo) {
        this.ticketRepo = ticketRepo;
        this.detalleRepo = detalleRepo;
        this.pedidoRepo = pedidoRepo;
    }

    @Transactional(readOnly = true)
    public List<DespachoPedidoDTO> obtenerPedidosDespacho() {
        List<TicketCocina> ticketsActivos = ticketRepo.findByEstadoInOrderByFechaHoraAsc(
                List.of(TicketCocina.EstadoTicket.IMPRESO, TicketCocina.EstadoTicket.PREPARANDO, TicketCocina.EstadoTicket.LISTO)
        );

        Map<Integer, List<TicketCocina>> ticketsPorPedidoId = new LinkedHashMap<>();
        Map<Integer, Pedido> pedidosMap = new LinkedHashMap<>();

        for (TicketCocina t : ticketsActivos) {
            if (t.getPedido() != null) {
                // Los pedidos DELIVERY no pasan por el despacho de mozos a la mesa
                if (t.getPedido().getTipo() == Pedido.TipoPedido.DELIVERY) {
                    continue;
                }
                Integer idPed = t.getPedido().getIdPedido();
                ticketsPorPedidoId.computeIfAbsent(idPed, k -> new ArrayList<>()).add(t);
                pedidosMap.putIfAbsent(idPed, t.getPedido());
            }
        }

        List<DespachoPedidoDTO> pedidosDespacho = new ArrayList<>();

        for (Map.Entry<Integer, List<TicketCocina>> entry : ticketsPorPedidoId.entrySet()) {
            Integer idPed = entry.getKey();
            Pedido pedido = pedidosMap.get(idPed);
            List<TicketCocina> tickets = entry.getValue();

            List<DetallePedido> detalles = detalleRepo.findByPedido_IdPedido(idPed).stream()
                    .filter(d -> d.getEstado() == DetallePedido.EstadoDetalle.EN_COCINA || d.getEstado() == DetallePedido.EstadoDetalle.LISTO)
                    .toList();

            if (detalles.isEmpty()) {
                continue;
            }

            pedidosDespacho.add(new DespachoPedidoDTO(pedido, tickets, detalles));
        }

        return pedidosDespacho;
    }

    @Transactional
    public Optional<String> entregarPedido(Integer idPedido) {
        return pedidoRepo.findById(idPedido).map(p -> {
            List<DetallePedido> detalles = detalleRepo.findByPedido_IdPedido(idPedido);
            for (DetallePedido d : detalles) {
                if (d.getEstado() == DetallePedido.EstadoDetalle.LISTO || d.getEstado() == DetallePedido.EstadoDetalle.EN_COCINA) {
                    d.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
                    detalleRepo.save(d);
                }
            }
            List<TicketCocina> tickets = ticketRepo.findByPedido_IdPedido(idPedido);
            for (TicketCocina t : tickets) {
                if (t.getEstado() != TicketCocina.EstadoTicket.ANULADO) {
                    t.setEstado(TicketCocina.EstadoTicket.ANULADO);
                    ticketRepo.save(t);
                }
            }
            return p.getMesa() != null ? "Mesa #" + p.getMesa().getNumero() : "Pedido " + p.getCodigo();
        });
    }

    @Transactional
    public Optional<TicketCocina> entregarTicket(Integer idTicket) {
        return ticketRepo.findById(idTicket).map(t -> {
            if (t.getDetallePedido() != null) {
                for (DetallePedido d : t.getDetallePedido()) {
                    d.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
                    detalleRepo.save(d);
                }
            }
            t.setEstado(TicketCocina.EstadoTicket.ANULADO);
            return ticketRepo.save(t);
        });
    }

    @Transactional
    public Optional<DetallePedido> entregarDetalle(Integer idDetalle) {
        return detalleRepo.findById(idDetalle).map(d -> {
            d.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
            return detalleRepo.save(d);
        });
    }

    /**
     * Valida si todos los platos y tickets de la comanda han sido completamente despachados y servidos a la mesa.
     */
    @Transactional(readOnly = true)
    public boolean estaTotalmenteDespachado(Integer idPedido) {
        if (idPedido == null) return false;
        Pedido p = pedidoRepo.findById(idPedido).orElse(null);
        if (p != null && p.getTipo() == Pedido.TipoPedido.DELIVERY) {
            return true;
        }
        List<DetallePedido> detalles = detalleRepo.findByPedido_IdPedido(idPedido);
        if (detalles.isEmpty()) return false;

        // Si existe algún plato que aún esté preparándose o listo en barra sin entregar
        boolean pendientesEntrega = detalles.stream().anyMatch(d ->
                d.getEstado() == DetallePedido.EstadoDetalle.EN_COCINA ||
                d.getEstado() == DetallePedido.EstadoDetalle.LISTO ||
                d.getEstado() == DetallePedido.EstadoDetalle.BORRADOR
        );
        if (pendientesEntrega) {
            return false;
        }

        // Si existe algún ticket en cocina que aún no haya sido completado y entregado
        List<TicketCocina> tickets = ticketRepo.findByPedido_IdPedido(idPedido);
        boolean ticketsActivos = tickets.stream().anyMatch(t ->
                t.getEstado() == TicketCocina.EstadoTicket.IMPRESO ||
                t.getEstado() == TicketCocina.EstadoTicket.PREPARANDO ||
                t.getEstado() == TicketCocina.EstadoTicket.LISTO
        );
        if (ticketsActivos) {
            return false;
        }

        // Al menos un plato debe haber sido marcado como ENTREGADO
        return detalles.stream().anyMatch(d -> d.getEstado() == DetallePedido.EstadoDetalle.ENTREGADO);
    }

    /**
     * Retorna una descripción detallada del motivo por el cual la comanda no está lista para precuenta o cobro.
     */
    @Transactional(readOnly = true)
    public String obtenerMotivoBloqueoDespacho(Integer idPedido) {
        if (idPedido == null) return "Pedido inválido";
        Pedido p = pedidoRepo.findById(idPedido).orElse(null);
        if (p != null && p.getTipo() == Pedido.TipoPedido.DELIVERY) {
            return "";
        }
        List<DetallePedido> detalles = detalleRepo.findByPedido_IdPedido(idPedido);
        if (detalles.isEmpty()) return "La comanda no tiene productos registrados";

        long enCocina = detalles.stream().filter(d -> d.getEstado() == DetallePedido.EstadoDetalle.EN_COCINA || d.getEstado() == DetallePedido.EstadoDetalle.BORRADOR).count();
        long listos = detalles.stream().filter(d -> d.getEstado() == DetallePedido.EstadoDetalle.LISTO).count();

        if (enCocina > 0) {
            return "Hay " + enCocina + " plato(s) en preparación en cocina";
        }
        if (listos > 0) {
            return "Hay " + listos + " plato(s) listos en barra esperando que el mozo confirme 'Servido / Entregado a Mesa'";
        }

        List<TicketCocina> tickets = ticketRepo.findByPedido_IdPedido(idPedido);
        long ticketsPendientes = tickets.stream().filter(t -> t.getEstado() != TicketCocina.EstadoTicket.ANULADO).count();
        if (ticketsPendientes > 0) {
            return "Cocina aún no ha finalizado los tickets de la comanda";
        }

        return "";
    }
}
