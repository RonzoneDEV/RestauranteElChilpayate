package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.controllers.CocinaController;
import com.cibertec.chilpayate.model.DetallePedido;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.TicketCocina;
import com.cibertec.chilpayate.repository.DetallePedidoRepository;
import com.cibertec.chilpayate.repository.PedidoDeliveryRepository;
import com.cibertec.chilpayate.repository.TicketCocinaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CocinaKdsTest {

    @Mock
    private TicketCocinaRepository ticketRepo;

    @Mock
    private DetallePedidoRepository detalleRepo;

    @Mock
    private PedidoDeliveryRepository deliveryRepo;

    private CocinaController cocinaController;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        CocinaService cocinaService = new CocinaService(ticketRepo, detalleRepo, deliveryRepo);
        cocinaController = new CocinaController(cocinaService);
    }

    @Test
    @DisplayName("TicketCocina.isPuedeCompletar solo es true cuando el ticket está PREPARANDO y todos sus platos están marcados como LISTO")
    void testPuedeCompletarLogica() {
        TicketCocina ticket = new TicketCocina();
        ticket.setIdTicket(1);
        ticket.setEstado(TicketCocina.EstadoTicket.IMPRESO);

        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(101);
        d1.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);

        DetallePedido d2 = new DetallePedido();
        d2.setIdDetallePedido(102);
        d2.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);

        ticket.setDetallePedido(List.of(d1, d2));

        // 1. En estado IMPRESO, debe estar bloqueado aunque los platos estuvieran listos
        assertFalse(ticket.isPuedeCompletar(), "En estado IMPRESO el botón completar debe estar bloqueado");

        // 2. Cambiamos a PREPARANDO pero los platos siguen EN_COCINA (desmarcados)
        ticket.setEstado(TicketCocina.EstadoTicket.PREPARANDO);
        assertFalse(ticket.isPuedeCompletar(), "En PREPARANDO con productos desmarcados debe estar bloqueado");

        // 3. Marcamos el primer plato (d1 listo, d2 pendiente)
        d1.setEstado(DetallePedido.EstadoDetalle.LISTO);
        assertFalse(ticket.isPuedeCompletar(), "Con solo algunos platos marcados debe seguir bloqueado");

        // 4. Marcamos el segundo plato (ambos listos)
        d2.setEstado(DetallePedido.EstadoDetalle.LISTO);
        assertTrue(ticket.isTodosListos());
        assertTrue(ticket.isPuedeCompletar(), "Cuando está PREPARANDO y todos los platos están marcados debe activarse");

        // 5. El cocinero desmarca un plato (vuelve a EN_COCINA)
        d2.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);
        assertFalse(ticket.isPuedeCompletar(), "Al desmarcar un plato, el botón debe bloquearse de nuevo inmediatamente");
    }

    @Test
    @DisplayName("Toggle de item en CocinaController NO autocompleta el ticket; el cocinero debe completarlo manualmente")
    void testToggleItemNoAutoCompletaTicket() {
        TicketCocina ticket = new TicketCocina();
        ticket.setIdTicket(10);
        ticket.setEstado(TicketCocina.EstadoTicket.PREPARANDO);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(20);
        ticket.setPedido(pedido);

        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(201);
        d1.setTicketCocina(ticket);
        d1.setPedido(pedido);
        d1.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);

        when(detalleRepo.findById(201)).thenReturn(Optional.of(d1));
        when(detalleRepo.findByTicketCocina_IdTicket(10)).thenReturn(List.of(d1));

        // Toggling d1 a LISTO
        Map<String, Object> resp = cocinaController.toggleItemListoAjax(201);

        assertTrue((Boolean) resp.get("success"));
        assertEquals("LISTO", resp.get("nuevoEstado"));
        assertTrue((Boolean) resp.get("todosListos"));

        // Verificar que NO se haya llamado a ticketRepo.save con LISTO
        verify(ticketRepo, never()).save(any(TicketCocina.class));
        assertEquals(TicketCocina.EstadoTicket.PREPARANDO, ticket.getEstado(), "El ticket debe permanecer en PREPARANDO sin cerrarse solo");
    }

    @Test
    @DisplayName("Endpoint AJAX para iniciar pedido bloquea y cambia ticket a PREPARANDO")
    void testIniciarPedidoAjax() {
        TicketCocina ticket = new TicketCocina();
        ticket.setIdTicket(5);
        ticket.setEstado(TicketCocina.EstadoTicket.IMPRESO);

        when(ticketRepo.findById(5)).thenReturn(Optional.of(ticket));

        Map<String, Object> resp = cocinaController.marcarPreparandoAjax(5);

        assertTrue((Boolean) resp.get("success"));
        assertEquals("PREPARANDO", resp.get("estado"));
        verify(ticketRepo).save(ticket);
        assertEquals(TicketCocina.EstadoTicket.PREPARANDO, ticket.getEstado());
    }

    @Test
    @DisplayName("Toggle de item en CocinaController está bloqueado si el ticket aún está en IMPRESO")
    void testToggleItemBloqueadoEnImpreso() {
        TicketCocina ticket = new TicketCocina();
        ticket.setIdTicket(30);
        ticket.setEstado(TicketCocina.EstadoTicket.IMPRESO);

        DetallePedido d = new DetallePedido();
        d.setIdDetallePedido(301);
        d.setTicketCocina(ticket);
        d.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);

        when(detalleRepo.findById(301)).thenReturn(Optional.of(d));

        Map<String, Object> resp = cocinaController.toggleItemListoAjax(301);

        assertFalse((Boolean) resp.get("success"));
        assertTrue(resp.get("error").toString().contains("Iniciar Pedido"));
        assertEquals(DetallePedido.EstadoDetalle.EN_COCINA, d.getEstado(), "El plato no debe cambiar de estado si el ticket no ha sido iniciado");
    }
}
