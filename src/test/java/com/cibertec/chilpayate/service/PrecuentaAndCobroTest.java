package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrecuentaAndCobroTest {

    @Mock
    private PrecuentaRepository precuentaRepo;

    @Mock
    private PrecuentaDetalleRepository precuentaDetalleRepo;

    @Mock
    private PedidoRepository pedidoRepo;

    @Mock
    private DetallePedidoRepository detallePedidoRepo;

    @Mock
    private PagoRepository pagoRepo;

    @Mock
    private PagoMedioRepository pagoMedioRepo;

    @Mock
    private PagoDetalleRepository pagoDetalleRepo;

    @Mock
    private VentaRepository ventaRepo;

    @Mock
    private MesaRepository mesaRepo;

    @Mock
    private MetodoPagoRepository metodoPagoRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private PedidoService pedidoService;

    @Mock
    private DespachoService despachoService;

    @InjectMocks
    private PrecuentaService precuentaService;

    @InjectMocks
    private CajaService cajaService;

    private Pedido pedido;
    private Mesa mesa;
    private DetallePedido det1;
    private DetallePedido det2;
    private MetodoPago metodoEfectivo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cajaService, "precuentaService", precuentaService);
        ReflectionTestUtils.setField(cajaService, "despachoService", despachoService);
        ReflectionTestUtils.setField(precuentaService, "despachoService", despachoService);
        ReflectionTestUtils.setField(precuentaService, "mesaRepo", mesaRepo);

        // Por defecto, asumir despacho completado salvo en pruebas de bloqueo
        when(despachoService.estaTotalmenteDespachado(anyInt())).thenReturn(true);

        mesa = new Mesa();
        mesa.setIdMesa(1);
        mesa.setNumero(5);
        mesa.setEstado(Mesa.EstadoMesa.OCUPADA);

        pedido = new Pedido();
        pedido.setIdPedido(100);
        pedido.setCodigo("PED-100");
        pedido.setMesa(mesa);
        pedido.setEstado(Pedido.EstadoPedido.ACTIVA);

        Producto prod1 = new Producto();
        prod1.setIdProducto(1);
        prod1.setNombre("Hamburguesa Clásica");

        det1 = new DetallePedido();
        det1.setIdDetallePedido(10);
        det1.setPedido(pedido);
        det1.setProducto(prod1);
        det1.setPrecioActual(new BigDecimal("25.00"));
        det1.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);

        Producto prod2 = new Producto();
        prod2.setIdProducto(2);
        prod2.setNombre("Alitas 12pz");

        det2 = new DetallePedido();
        det2.setIdDetallePedido(20);
        det2.setPedido(pedido);
        det2.setProducto(prod2);
        det2.setPrecioActual(new BigDecimal("35.00"));
        det2.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);

        pedido.setDetallePedido(List.of(det1, det2));

        metodoEfectivo = new MetodoPago();
        metodoEfectivo.setIdMetodoPago(1);
        metodoEfectivo.setNombre("Efectivo");
        metodoEfectivo.setEstado(true);
    }

    @Test
    void testGenerarPrecuentaDivididaPorProductoYBloquearMesa() {
        when(pedidoRepo.findById(100)).thenReturn(Optional.of(pedido));
        when(precuentaRepo.findByPedido_IdPedidoAndEstadoNot(100, Precuenta.EstadoPrecuenta.ANULADA)).thenReturn(Collections.emptyList());
        when(precuentaRepo.findByPedido_IdPedidoOrderByNumeroPrecuentaAsc(100)).thenReturn(Collections.emptyList());
        when(detallePedidoRepo.findById(10)).thenReturn(Optional.of(det1));

        when(precuentaRepo.save(any(Precuenta.class))).thenAnswer(invocation -> {
            Precuenta p = invocation.getArgument(0);
            p.setIdPrecuenta(1);
            return p;
        });

        // Generar precuenta solo con det1 (Hamburguesa Clásica S/ 25.00)
        Precuenta p1 = precuentaService.generarPrecuentaDividida(100, List.of(10));

        assertNotNull(p1);
        assertEquals(new BigDecimal("25.00"), p1.getTotal());
        assertEquals(1, p1.getNumeroPrecuenta());
        assertEquals(Pedido.EstadoPedido.PRECUENTA, pedido.getEstado());
        // Regla de Negocio: Mesa pasa a BLOQUEADA
        assertEquals(Mesa.EstadoMesa.BLOQUEADA, mesa.getEstado());
        verify(mesaRepo, atLeastOnce()).save(mesa);

        verify(precuentaDetalleRepo, times(1)).save(any(PrecuentaDetalle.class));
    }

    @Test
    void testBloqueoPrecuentaSiNoEstaDespachado() {
        when(pedidoRepo.findById(100)).thenReturn(Optional.of(pedido));
        when(despachoService.estaTotalmenteDespachado(100)).thenReturn(false);
        when(despachoService.obtenerMotivoBloqueoDespacho(100)).thenReturn("Hay 1 plato en preparación en cocina");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                precuentaService.generarPrecuentaDividida(100, List.of(10), "Cliente A")
        );
        assertTrue(ex.getMessage().contains("No se puede generar la precuenta"));
        assertTrue(ex.getMessage().contains("preparación en cocina"));
    }

    @Test
    void testBloqueoCobroSiNoEstaDespachado() {
        Precuenta prec1 = new Precuenta();
        prec1.setIdPrecuenta(1);
        prec1.setPedido(pedido);
        prec1.setTotal(new BigDecimal("25.00"));
        prec1.setEstado(Precuenta.EstadoPrecuenta.PENDIENTE);

        when(precuentaRepo.findById(1)).thenReturn(Optional.of(prec1));
        when(despachoService.estaTotalmenteDespachado(100)).thenReturn(false);
        when(despachoService.obtenerMotivoBloqueoDespacho(100)).thenReturn("Hay platos pendientes de entrega por el mozo");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                cajaService.procesarPagoPrecuenta(1, null, List.of(new CajaService.PagoItemDto(1, new BigDecimal("25.00"), null)), "BOLETA", "B001")
        );
        assertTrue(ex.getMessage().contains("No se puede cobrar la precuenta"));
    }

    @Test
    void testBloqueoCobroSiNoTienePrecuentaGenerada() {
        when(pedidoRepo.findById(100)).thenReturn(Optional.of(pedido));
        when(despachoService.estaTotalmenteDespachado(100)).thenReturn(true);
        when(precuentaRepo.findByPedido_IdPedidoAndEstadoNot(100, Precuenta.EstadoPrecuenta.ANULADA)).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                cajaService.procesarPagoMulti(100, null, List.of(new CajaService.PagoItemDto(1, new BigDecimal("60.00"), null)), "BOLETA", "B001")
        );
        assertTrue(ex.getMessage().contains("Primero se debe generar la precuenta"));
    }

    @Test
    void testCobrarPrecuentaIndividualYLiberarMesaAlFinal() {
        // Precuenta 1 (S/ 25.00)
        Precuenta prec1 = new Precuenta();
        prec1.setIdPrecuenta(1);
        prec1.setPedido(pedido);
        prec1.setNumeroPrecuenta(1);
        prec1.setTotal(new BigDecimal("25.00"));
        prec1.setEstado(Precuenta.EstadoPrecuenta.PENDIENTE);

        PrecuentaDetalle pd1 = new PrecuentaDetalle(prec1, det1, new BigDecimal("25.00"));
        prec1.setDetalles(List.of(pd1));

        // Precuenta 2 (S/ 35.00)
        Precuenta prec2 = new Precuenta();
        prec2.setIdPrecuenta(2);
        prec2.setPedido(pedido);
        prec2.setNumeroPrecuenta(2);
        prec2.setTotal(new BigDecimal("35.00"));
        prec2.setEstado(Precuenta.EstadoPrecuenta.PENDIENTE);

        when(precuentaRepo.findById(1)).thenReturn(Optional.of(prec1));
        when(precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(1)).thenReturn(List.of(pd1));
        when(metodoPagoRepo.findById(1)).thenReturn(Optional.of(metodoEfectivo));

        when(pagoRepo.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago p = invocation.getArgument(0);
            p.setIdPago(501);
            return p;
        });

        when(ventaRepo.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setIdVenta(601);
            return v;
        });

        // Simular que todavía queda Precuenta 2 pendiente
        when(precuentaRepo.findByPedido_IdPedidoAndEstado(100, Precuenta.EstadoPrecuenta.PENDIENTE))
                .thenReturn(List.of(prec2));

        // Cobrar Precuenta 1
        CajaService.PagoItemDto medioPago = new CajaService.PagoItemDto(1, new BigDecimal("25.00"), null);
        Venta venta = cajaService.procesarPagoPrecuenta(1, null, List.of(medioPago), "BOLETA", "B001-0001");

        assertNotNull(venta);
        assertEquals(new BigDecimal("25.00"), venta.getTotal());
        assertEquals(Precuenta.EstadoPrecuenta.PAGADA, prec1.getEstado());
        // Como prec2 sigue pendiente, la comanda NO se finaliza aún
        assertNotEquals(Pedido.EstadoPedido.PAGADA, pedido.getEstado());

        // Ahora cobrar Precuenta 2
        when(precuentaRepo.findById(2)).thenReturn(Optional.of(prec2));
        PrecuentaDetalle pd2 = new PrecuentaDetalle(prec2, det2, new BigDecimal("35.00"));
        prec2.setDetalles(List.of(pd2));
        when(precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(2)).thenReturn(List.of(pd2));

        // Tras cobrar prec2, no quedan pendientes ni productos disponibles
        when(precuentaRepo.findByPedido_IdPedidoAndEstado(100, Precuenta.EstadoPrecuenta.PENDIENTE))
                .thenReturn(Collections.emptyList());

        // Simular que precuentaRepo ve prec1 y prec2 como no anuladas
        when(precuentaRepo.findByPedido_IdPedidoAndEstadoNot(100, Precuenta.EstadoPrecuenta.ANULADA))
                .thenReturn(List.of(prec1, prec2));
        when(precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(2)).thenReturn(List.of(pd2));

        Venta venta2 = cajaService.procesarPagoPrecuenta(2, null, List.of(new CajaService.PagoItemDto(1, new BigDecimal("35.00"), null)), "BOLETA", "B001-0002");

        assertNotNull(venta2);
        assertEquals(Precuenta.EstadoPrecuenta.PAGADA, prec2.getEstado());
        // Ahora sí la comanda se cierra y la mesa se libera
        assertEquals(Pedido.EstadoPedido.PAGADA, pedido.getEstado());
        assertEquals(Mesa.EstadoMesa.LIBRE, mesa.getEstado());
    }

    @Test
    void testCobrarPrecuentaConClienteYDniRucYTrazabilidad() {
        Precuenta prec = new Precuenta();
        prec.setIdPrecuenta(10);
        prec.setPedido(pedido);
        prec.setNumeroPrecuenta(1);
        prec.setTotal(new BigDecimal("25.00"));
        prec.setEstado(Precuenta.EstadoPrecuenta.PENDIENTE);

        PrecuentaDetalle pd = new PrecuentaDetalle(prec, det1, new BigDecimal("25.00"));
        prec.setDetalles(List.of(pd));

        when(precuentaRepo.findById(10)).thenReturn(Optional.of(prec));
        when(precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(10)).thenReturn(List.of(pd));
        when(metodoPagoRepo.findById(1)).thenReturn(Optional.of(metodoEfectivo));

        when(pagoRepo.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago p = invocation.getArgument(0);
            p.setIdPago(900);
            return p;
        });

        when(ventaRepo.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setIdVenta(901);
            return v;
        });

        Venta v = cajaService.procesarPagoPrecuenta(10, null,
                List.of(new CajaService.PagoItemDto(1, new BigDecimal("25.00"), "OP-123")),
                "BOLETA", "B001-00099", "Carlos Mendoza", "72345678");

        assertNotNull(v);
        assertEquals("Carlos Mendoza", v.getNombreCliente());
        assertEquals("72345678", v.getDniRuc());
        assertEquals("BOLETA", v.getTipoComprobante());
        assertEquals(prec, v.getPrecuenta());
        assertNotNull(v.getPago());
        assertEquals(900, v.getPago().getIdPago());
        assertEquals(prec, v.getPago().getPrecuenta());
        assertEquals(Precuenta.EstadoPrecuenta.PAGADA, prec.getEstado());
        assertEquals(v.getPago(), prec.getPago());
    }
}
