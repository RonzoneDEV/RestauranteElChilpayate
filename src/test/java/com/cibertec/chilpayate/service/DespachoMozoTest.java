package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.controllers.DespachoController;
import com.cibertec.chilpayate.controllers.DespachoController.DespachoPedidoDTO;
import com.cibertec.chilpayate.model.DetallePedido;
import com.cibertec.chilpayate.model.Mesa;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.TicketCocina;
import com.cibertec.chilpayate.repository.DetallePedidoRepository;
import com.cibertec.chilpayate.repository.PedidoRepository;
import com.cibertec.chilpayate.repository.TicketCocinaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DespachoMozoTest {

    @Mock
    private TicketCocinaRepository ticketRepo;

    @Mock
    private DetallePedidoRepository detalleRepo;

    @Mock
    private PedidoRepository pedidoRepo;

    @Mock
    private RedirectAttributes redirectAttributes;

    private DespachoController despachoController;
    private DespachoService despachoService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        despachoService = new DespachoService(ticketRepo, detalleRepo, pedidoRepo);
        despachoController = new DespachoController(despachoService);
    }

    @Test
    @DisplayName("DespachoPedidoDTO bloquea el botón de entrega hasta que TODOS los productos y tickets estén listos")
    void testDespachoPedidoBloqueoConUnTicket() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(1);
        Mesa mesa = new Mesa();
        mesa.setNumero(5);
        pedido.setMesa(mesa);

        TicketCocina ticket1 = new TicketCocina();
        ticket1.setIdTicket(101);
        ticket1.setCorrelativo(1);
        ticket1.setEstado(TicketCocina.EstadoTicket.PREPARANDO);

        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(11);
        d1.setTicketCocina(ticket1);
        d1.setEstado(DetallePedido.EstadoDetalle.EN_COCINA); // preparando

        DetallePedido d2 = new DetallePedido();
        d2.setIdDetallePedido(12);
        d2.setTicketCocina(ticket1);
        d2.setEstado(DetallePedido.EstadoDetalle.LISTO); // listo

        // 1. Productos parcialmente listos y ticket aún no completado
        DespachoPedidoDTO dto = new DespachoPedidoDTO(pedido, List.of(ticket1), List.of(d1, d2));
        assertFalse(dto.isTodosProductosListos(), "No todos los productos están listos");
        assertFalse(dto.isTodosTicketsCompletados(), "El ticket aún no fue completado por cocina");
        assertFalse(dto.isPuedeServir(), "El botón de servido / entregado debe estar bloqueado");
        assertTrue(dto.getMotivoBloqueo().contains("Cocina aún no ha completado el ticket"));

        // 2. Cocina marca ambos platos como listos pero aún no da clic a Pedido Completado
        d1.setEstado(DetallePedido.EstadoDetalle.LISTO);
        dto = new DespachoPedidoDTO(pedido, List.of(ticket1), List.of(d1, d2));
        assertTrue(dto.isTodosProductosListos());
        assertFalse(dto.isTodosTicketsCompletados());
        assertFalse(dto.isPuedeServir(), "Sigue bloqueado porque el ticket no ha sido completado");

        // 3. Cocina completa el ticket
        ticket1.setEstado(TicketCocina.EstadoTicket.LISTO);
        dto = new DespachoPedidoDTO(pedido, List.of(ticket1), List.of(d1, d2));
        assertTrue(dto.isTodosProductosListos());
        assertTrue(dto.isTodosTicketsCompletados());
        assertTrue(dto.isPuedeServir(), "Ahora sí debe habilitarse el botón de Servido / Entregado");
    }

    @Test
    @DisplayName("Si el mozo agrega otro ticket, deben completarse TODOS los tickets para poder activar el botón")
    void testDespachoBloqueoConMultiplesTickets() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(2);
        Mesa mesa = new Mesa();
        mesa.setNumero(3);
        pedido.setMesa(mesa);

        // Ticket 1: completado
        TicketCocina ticket1 = new TicketCocina();
        ticket1.setIdTicket(201);
        ticket1.setCorrelativo(1);
        ticket1.setEstado(TicketCocina.EstadoTicket.LISTO);

        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(21);
        d1.setTicketCocina(ticket1);
        d1.setEstado(DetallePedido.EstadoDetalle.LISTO);

        // Ticket 2: agregado por el mozo, aún en cola / preparando en cocina
        TicketCocina ticket2 = new TicketCocina();
        ticket2.setIdTicket(202);
        ticket2.setCorrelativo(2);
        ticket2.setEstado(TicketCocina.EstadoTicket.IMPRESO);

        DetallePedido d2 = new DetallePedido();
        d2.setIdDetallePedido(22);
        d2.setTicketCocina(ticket2);
        d2.setEstado(DetallePedido.EstadoDetalle.EN_COCINA); // preparando

        // Con 2 tickets y el 2do pendiente
        DespachoPedidoDTO dto = new DespachoPedidoDTO(pedido, List.of(ticket1, ticket2), List.of(d1, d2));

        assertEquals(2, dto.getTotalTickets());
        assertEquals(1, dto.getTicketsListos());
        assertFalse(dto.isTodosTicketsCompletados(), "Falta completar el ticket 2");
        assertFalse(dto.isPuedeServir(), "El botón debe estar bloqueado mientras no se completen todos los tickets");
        assertTrue(dto.getMotivoBloqueo().contains("Primero cocina debe completar todos los tickets"));

        // Cocina prepara y completa el Ticket 2
        d2.setEstado(DetallePedido.EstadoDetalle.LISTO);
        ticket2.setEstado(TicketCocina.EstadoTicket.LISTO);

        DespachoPedidoDTO dtoCompleto = new DespachoPedidoDTO(pedido, List.of(ticket1, ticket2), List.of(d1, d2));
        assertEquals(2, dtoCompleto.getTicketsListos());
        assertTrue(dtoCompleto.isTodosTicketsCompletados());
        assertTrue(dtoCompleto.isTodosProductosListos());
        assertTrue(dtoCompleto.isPuedeServir(), "Al completarse todos los tickets, el botón se activa");
    }

    @Test
    @DisplayName("entregarPedido marca todos los detalles como ENTREGADO y archiva los tickets")
    void testEntregarPedido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(50);
        Mesa mesa = new Mesa();
        mesa.setNumero(8);
        pedido.setMesa(mesa);

        TicketCocina t1 = new TicketCocina();
        t1.setIdTicket(501);
        t1.setEstado(TicketCocina.EstadoTicket.LISTO);

        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(51);
        d1.setEstado(DetallePedido.EstadoDetalle.LISTO);

        when(pedidoRepo.findById(50)).thenReturn(Optional.of(pedido));
        when(detalleRepo.findByPedido_IdPedido(50)).thenReturn(List.of(d1));
        when(ticketRepo.findByPedido_IdPedido(50)).thenReturn(List.of(t1));

        String redirect = despachoController.entregarPedido(50, redirectAttributes);

        assertEquals("redirect:/despacho", redirect);
        assertEquals(DetallePedido.EstadoDetalle.ENTREGADO, d1.getEstado());
        assertEquals(TicketCocina.EstadoTicket.ANULADO, t1.getEstado());
        verify(detalleRepo).save(d1);
        verify(ticketRepo).save(t1);
    }

    @Test
    @DisplayName("estaTotalmenteDespachado retorna true solo cuando todos los platos están en ENTREGADO y no hay tickets activos")
    void testEstaTotalmenteDespachado() {
        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(1);
        d1.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);

        when(detalleRepo.findByPedido_IdPedido(10)).thenReturn(List.of(d1));
        assertFalse(despachoService.estaTotalmenteDespachado(10), "Falso porque d1 está EN_COCINA");

        d1.setEstado(DetallePedido.EstadoDetalle.LISTO);
        assertFalse(despachoService.estaTotalmenteDespachado(10), "Falso porque d1 está LISTO pero no ENTREGADO");

        d1.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        TicketCocina t1 = new TicketCocina();
        t1.setEstado(TicketCocina.EstadoTicket.LISTO);
        when(ticketRepo.findByPedido_IdPedido(10)).thenReturn(List.of(t1));
        assertFalse(despachoService.estaTotalmenteDespachado(10), "Falso porque el ticket sigue activo en cocina");

        t1.setEstado(TicketCocina.EstadoTicket.ANULADO);
        assertTrue(despachoService.estaTotalmenteDespachado(10), "Verdadero porque platos entregados y tickets cerrados");
    }
}
