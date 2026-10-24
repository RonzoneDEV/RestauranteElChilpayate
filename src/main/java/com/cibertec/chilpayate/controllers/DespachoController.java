package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.DetallePedido;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.TicketCocina;
import com.cibertec.chilpayate.service.DespachoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/despacho")
public class DespachoController {

    private final DespachoService despachoService;

    public DespachoController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    public static class DespachoPedidoDTO {
        private Pedido pedido;
        private List<TicketCocina> tickets;
        private List<DetallePedido> detalles;
        private int totalProductos;
        private int productosListos;
        private int productosPreparando;
        private int totalTickets;
        private int ticketsListos;
        private boolean todosTicketsCompletados;
        private boolean todosProductosListos;
        private boolean puedeServir;
        private String motivoBloqueo;

        public DespachoPedidoDTO(Pedido pedido, List<TicketCocina> tickets, List<DetallePedido> detalles) {
            this.pedido = pedido;
            this.tickets = tickets != null ? tickets : Collections.emptyList();
            this.detalles = detalles != null ? detalles : Collections.emptyList();

            this.totalTickets = this.tickets.size();
            this.ticketsListos = (int) this.tickets.stream()
                    .filter(t -> t.getEstado() == TicketCocina.EstadoTicket.LISTO)
                    .count();
            this.todosTicketsCompletados = (this.totalTickets > 0 && this.ticketsListos == this.totalTickets);

            this.totalProductos = this.detalles.size();
            this.productosListos = (int) this.detalles.stream()
                    .filter(d -> d.getEstado() == DetallePedido.EstadoDetalle.LISTO)
                    .count();
            this.productosPreparando = this.totalProductos - this.productosListos;
            this.todosProductosListos = (this.totalProductos > 0 && this.productosListos == this.totalProductos);

            this.puedeServir = this.todosTicketsCompletados && this.todosProductosListos;

            if (!this.todosTicketsCompletados && this.totalTickets > 1) {
                int ticketsFaltantes = this.totalTickets - this.ticketsListos;
                this.motivoBloqueo = "Hay " + this.totalTickets + " tickets para este pedido. Primero cocina debe completar todos los tickets (" + ticketsFaltantes + " pendiente" + (ticketsFaltantes > 1 ? "s" : "") + ").";
            } else if (!this.todosTicketsCompletados) {
                this.motivoBloqueo = "Cocina aún no ha completado el ticket de este pedido.";
            } else if (!this.todosProductosListos) {
                this.motivoBloqueo = "Faltan " + this.productosPreparando + " plato(s) por marcar como listo(s) en cocina.";
            } else {
                this.motivoBloqueo = "";
            }
        }

        public Pedido getPedido() { return pedido; }
        public List<TicketCocina> getTickets() { return tickets; }
        public List<DetallePedido> getDetalles() { return detalles; }
        public int getTotalProductos() { return totalProductos; }
        public int getProductosListos() { return productosListos; }
        public int getProductosPreparando() { return productosPreparando; }
        public int getTotalTickets() { return totalTickets; }
        public int getTicketsListos() { return ticketsListos; }
        public boolean isTodosTicketsCompletados() { return todosTicketsCompletados; }
        public boolean isTodosProductosListos() { return todosProductosListos; }
        public boolean isPuedeServir() { return puedeServir; }
        public String getMotivoBloqueo() { return motivoBloqueo; }
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pedidosDespacho", despachoService.obtenerPedidosDespacho());
        return "despacho/index";
    }

    @PostMapping("/entregar-pedido/{id}")
    public String entregarPedido(@PathVariable Integer id, RedirectAttributes ra) {
        despachoService.entregarPedido(id).ifPresent(destino ->
                ra.addFlashAttribute("success", "Comanda de " + destino + " servida y entregada exitosamente.")
        );
        return "redirect:/despacho";
    }

    @PostMapping("/entregar-ticket/{id}")
    public String entregarTicket(@PathVariable Integer id, RedirectAttributes ra) {
        despachoService.entregarTicket(id).ifPresent(t ->
                ra.addFlashAttribute("success", "Ticket #" + t.getCorrelativo() + " servido y entregado a la mesa.")
        );
        return "redirect:/despacho";
    }

    @PostMapping("/entregar-detalle/{id}")
    public String entregarDetalle(@PathVariable Integer id, RedirectAttributes ra) {
        despachoService.entregarDetalle(id).ifPresent(d ->
                ra.addFlashAttribute("success", "Plato '" + (d.getProducto() != null ? d.getProducto().getNombre() : "") + "' marcado como entregado.")
        );
        return "redirect:/despacho";
    }
}
