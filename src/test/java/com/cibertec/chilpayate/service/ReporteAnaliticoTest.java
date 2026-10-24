package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.dto.ReporteAnaliticoDto;
import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteAnaliticoTest {

    @Mock
    private VentaRepository ventaRepo;
    @Mock
    private GastoRepository gastoRepo;
    @Mock
    private PedidoRepository pedidoRepo;
    @Mock
    private ProductoRepository productoRepo;
    @Mock
    private DetallePedidoRepository detallePedidoRepo;
    @Mock
    private DetallePedidoSalsaRepository detallePedidoSalsaRepo;
    @Mock
    private PagoMedioRepository pagoMedioRepo;
    @Mock
    private ConfiguracionService configuracionService;

    @InjectMocks
    private ReporteService reporteService;

    private Usuario mozo1;
    private Usuario mozo2;
    private Categoria catAlitas;
    private Categoria catBebidas;
    private Producto prodAlitas12;
    private Producto prodAlitas6;
    private Producto prodGaseosa;
    private Producto salsaBbq;
    private Producto salsaAcevichada;

    @BeforeEach
    void setUp() {
        Rol rolMozo = new Rol();
        rolMozo.setIdRol(2);
        rolMozo.setNombre("MOZO");

        mozo1 = new Usuario();
        mozo1.setIdUsuario(10);
        mozo1.setNombres("Carlos");
        mozo1.setApellidos("Mozo");
        mozo1.setUsername("carlosm");
        mozo1.setRol(rolMozo);

        mozo2 = new Usuario();
        mozo2.setIdUsuario(20);
        mozo2.setNombres("Ana");
        mozo2.setApellidos("Mesera");
        mozo2.setUsername("anam");
        mozo2.setRol(rolMozo);

        catAlitas = new Categoria();
        catAlitas.setIdCategoria(1);
        catAlitas.setNombre("Alitas");

        catBebidas = new Categoria();
        catBebidas.setIdCategoria(2);
        catBebidas.setNombre("Bebidas");

        prodAlitas12 = new Producto();
        prodAlitas12.setIdProducto(101);
        prodAlitas12.setNombre("Alitas Combo x 12");
        prodAlitas12.setCategoria(catAlitas);
        prodAlitas12.setPrecio(new BigDecimal("34.00"));

        prodAlitas6 = new Producto();
        prodAlitas6.setIdProducto(102);
        prodAlitas6.setNombre("Alitas Combo x6");
        prodAlitas6.setCategoria(catAlitas);
        prodAlitas6.setPrecio(new BigDecimal("18.00"));

        prodGaseosa = new Producto();
        prodGaseosa.setIdProducto(201);
        prodGaseosa.setNombre("Inka Kola 500ml");
        prodGaseosa.setCategoria(catBebidas);
        prodGaseosa.setPrecio(new BigDecimal("5.00"));

        salsaBbq = new Producto();
        salsaBbq.setIdProducto(301);
        salsaBbq.setNombre("BBQ Ahumada");

        salsaAcevichada = new Producto();
        salsaAcevichada.setIdProducto(302);
        salsaAcevichada.setNombre("Acevichada");
    }

    @Test
    @DisplayName("Debe calcular correctamente el ranking de Alitas y piezas estimadas")
    void testRankingAlitas() {
        LocalDate hoy = LocalDate.now();

        // 3 pedidos de Alitas x12 y 2 pedidos de Alitas x6
        Pedido ped1 = new Pedido();
        ped1.setIdPedido(1);
        ped1.setFecha(LocalDateTime.now().withHour(19));
        ped1.setEstado(Pedido.EstadoPedido.PAGADA);
        ped1.setTipo(Pedido.TipoPedido.SALON);
        ped1.setUsuario(mozo1);

        DetallePedido d1 = new DetallePedido();
        d1.setIdDetallePedido(1);
        d1.setProducto(prodAlitas12);
        d1.setPrecioActual(new BigDecimal("34.00"));
        d1.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        d1.setPedido(ped1);

        DetallePedido d2 = new DetallePedido();
        d2.setIdDetallePedido(2);
        d2.setProducto(prodAlitas12);
        d2.setPrecioActual(new BigDecimal("34.00"));
        d2.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        d2.setPedido(ped1);

        DetallePedido d3 = new DetallePedido();
        d3.setIdDetallePedido(3);
        d3.setProducto(prodAlitas6);
        d3.setPrecioActual(new BigDecimal("18.00"));
        d3.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        d3.setPedido(ped1);

        ped1.setDetallePedido(List.of(d1, d2, d3));

        when(pedidoRepo.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of(ped1));
        when(detallePedidoRepo.findDetallesValidosEntreFechas(any(), any())).thenReturn(List.of(d1, d2, d3));
        when(detallePedidoSalsaRepo.findSalsasValidasEntreFechas(any(), any())).thenReturn(List.of());
        when(pagoMedioRepo.findPagosMediosEntreFechas(any(), any())).thenReturn(List.of());

        ReporteAnaliticoDto dto = reporteService.obtenerReporteAnalitico(hoy, hoy);

        assertNotNull(dto);
        assertEquals(2, dto.getTopAlitas().size());
        assertEquals("Alitas Combo x 12", dto.getAlitaMasVendida());
        assertEquals(2, dto.getAlitaMasVendidaCant());
        // 2 combos x 12 = 24 piezas de alitas
        assertEquals(24, dto.getAlitaMasVendidaPiezas());
        // Total porciones alitas: 2 + 1 = 3 porciones
        assertEquals(3, dto.getTotalPorcionesAlitas());
        // Total piezas: 24 + 6 = 30 piezas
        assertEquals(30, dto.getTotalPiezasAlitas());
    }

    @Test
    @DisplayName("Debe identificar al mozo estrella con más platos vendidos y recaudación")
    void testMozoEstrella() {
        LocalDate hoy = LocalDate.now();

        Pedido p1 = new Pedido();
        p1.setIdPedido(1);
        p1.setFecha(LocalDateTime.now().withHour(20));
        p1.setEstado(Pedido.EstadoPedido.PAGADA);
        p1.setTipo(Pedido.TipoPedido.SALON);
        p1.setUsuario(mozo1);

        DetallePedido d1 = new DetallePedido();
        d1.setProducto(prodAlitas12);
        d1.setPrecioActual(new BigDecimal("34.00"));
        d1.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        d1.setPedido(p1);

        DetallePedido d2 = new DetallePedido();
        d2.setProducto(prodGaseosa);
        d2.setPrecioActual(new BigDecimal("5.00"));
        d2.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        d2.setPedido(p1);

        p1.setDetallePedido(List.of(d1, d2));

        Pedido p2 = new Pedido();
        p2.setIdPedido(2);
        p2.setFecha(LocalDateTime.now().withHour(21));
        p2.setEstado(Pedido.EstadoPedido.PAGADA);
        p2.setTipo(Pedido.TipoPedido.SALON);
        p2.setUsuario(mozo2);

        DetallePedido d3 = new DetallePedido();
        d3.setProducto(prodGaseosa);
        d3.setPrecioActual(new BigDecimal("5.00"));
        d3.setEstado(DetallePedido.EstadoDetalle.ENTREGADO);
        d3.setPedido(p2);

        p2.setDetallePedido(List.of(d3));

        when(pedidoRepo.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of(p1, p2));
        when(detallePedidoRepo.findDetallesValidosEntreFechas(any(), any())).thenReturn(List.of(d1, d2, d3));
        when(detallePedidoSalsaRepo.findSalsasValidasEntreFechas(any(), any())).thenReturn(List.of());
        when(pagoMedioRepo.findPagosMediosEntreFechas(any(), any())).thenReturn(List.of());

        ReporteAnaliticoDto dto = reporteService.obtenerReporteAnalitico(hoy, hoy);

        assertEquals("Carlos Mozo", dto.getMozoEstrella());
        assertEquals(2, dto.getMozoEstrellaPlatos());
        assertEquals(new BigDecimal("39.00"), dto.getMozoEstrellaMonto());
    }

    @Test
    @DisplayName("Debe calcular correctamente el ranking de salsas más pedidas")
    void testRankingSalsas() {
        LocalDate hoy = LocalDate.now();

        DetallePedidoSalsa s1 = new DetallePedidoSalsa();
        s1.setIdDetalleSalsa(1);
        s1.setSalsa(salsaBbq);

        DetallePedidoSalsa s2 = new DetallePedidoSalsa();
        s2.setIdDetalleSalsa(2);
        s2.setSalsa(salsaBbq);

        DetallePedidoSalsa s3 = new DetallePedidoSalsa();
        s3.setIdDetalleSalsa(3);
        s3.setSalsa(salsaAcevichada);

        when(pedidoRepo.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of());
        when(detallePedidoRepo.findDetallesValidosEntreFechas(any(), any())).thenReturn(List.of());
        when(detallePedidoSalsaRepo.findSalsasValidasEntreFechas(any(), any())).thenReturn(List.of(s1, s2, s3));
        when(pagoMedioRepo.findPagosMediosEntreFechas(any(), any())).thenReturn(List.of());

        ReporteAnaliticoDto dto = reporteService.obtenerReporteAnalitico(hoy, hoy);

        assertEquals("BBQ Ahumada", dto.getSalsaMasPedida());
        assertEquals(2, dto.getSalsaMasPedidaCant());
        assertEquals(2, dto.getTopSalsas().size());
        assertEquals("Acevichada", dto.getTopSalsas().get(1).getNombre());
        assertEquals(1, dto.getTopSalsas().get(1).getVecesPedida());
    }

    @Test
    @DisplayName("Debe compilar y generar el PDF de indicadores y KPIs en JasperReports")
    void testGenerarPdfIndicadoresJasperReports() throws Exception {
        LocalDate hoy = LocalDate.now();

        Configuracion conf = new Configuracion();
        conf.setNombre("El Chilpayate");
        conf.setRuc("10876343874");
        conf.setDireccion("Carlos Wiesse 625");
        conf.setDistrito("Trujillo");

        when(configuracionService.obtenerConfiguracion()).thenReturn(conf);
        when(pedidoRepo.findByFechaBetweenOrderByFechaDesc(any(), any())).thenReturn(List.of());
        when(detallePedidoRepo.findDetallesValidosEntreFechas(any(), any())).thenReturn(List.of());
        when(detallePedidoSalsaRepo.findSalsasValidasEntreFechas(any(), any())).thenReturn(List.of());
        when(pagoMedioRepo.findPagosMediosEntreFechas(any(), any())).thenReturn(List.of());

        byte[] pdfBytes = reporteService.generarPdfIndicadores(hoy, hoy);

        assertNotNull(pdfBytes, "El PDF generado no debe ser nulo");
        assertTrue(pdfBytes.length > 0, "El PDF debe contener bytes");

        // Los archivos PDF válidos inician con la cabecera '%PDF-'
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header, "El archivo generado debe ser un PDF válido");
    }
}
