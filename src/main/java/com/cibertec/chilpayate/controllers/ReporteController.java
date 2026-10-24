package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.dto.ReporteAnaliticoDto;
import com.cibertec.chilpayate.model.Gasto;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.Venta;
import com.cibertec.chilpayate.service.ReporteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        Model model) {
        if (desde == null) desde = LocalDate.now().minusDays(7);
        if (hasta == null) hasta = LocalDate.now();

        List<Venta> ventas = reporteService.listarVentasPorRango(desde, hasta);
        List<Gasto> gastos = reporteService.listarGastosPorRango(desde, hasta);
        List<Pedido> pedidos = reporteService.listarPedidosPorRango(desde, hasta);

        // 1. Total Ventas Cobradas (en caja con comprobante emitido)
        BigDecimal totalVentasCobradas = ventas.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getEstado()) && v.getTotal() != null)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Si no hay ventas con comprobante pero hay pedidos pagados
        if (totalVentasCobradas.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal pedidosPagados = pedidos.stream()
                    .filter(p -> p != null && (p.getEstado() == Pedido.EstadoPedido.PAGADA) && p.getTotal() != null)
                    .map(Pedido::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalVentasCobradas = pedidosPagados;
        }

        // 2. Total General de Pedidos / Comandas del período (todas las comandas válidas generadas)
        BigDecimal totalPedidosMonto = pedidos.stream()
                .filter(p -> p != null && p.getEstado() != Pedido.EstadoPedido.ANULADA && p.getTotal() != null)
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Total Consumo Pendiente en Salón (mesas en consumo o precuenta por cobrar)
        BigDecimal totalConsumoPendiente = pedidos.stream()
                .filter(p -> p != null && p.getEstado() != Pedido.EstadoPedido.PAGADA && p.getEstado() != Pedido.EstadoPedido.ANULADA && p.getTotal() != null)
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Total Gastos
        BigDecimal totalGastos = gastos.stream()
                .filter(g -> g != null && g.getMonto() != null)
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Total Ventas para la tarjeta principal:
        // Si hay ventas cobradas, muestra las cobradas; si aún están en consumo, muestra el total de pedidos registrados
        BigDecimal totalVentas = (totalVentasCobradas.compareTo(BigDecimal.ZERO) > 0)
                ? totalVentasCobradas
                : totalPedidosMonto;

        BigDecimal utilidad = totalVentas.subtract(totalGastos);

        ReporteAnaliticoDto analitica = reporteService.obtenerReporteAnalitico(desde, hasta);

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("resumen", reporteService.resumenGeneral());
        model.addAttribute("ventas", ventas);
        model.addAttribute("gastos", gastos);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("analitica", analitica);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("totalVentasCobradas", totalVentasCobradas);
        model.addAttribute("totalPedidosMonto", totalPedidosMonto);
        model.addAttribute("totalGastos", totalGastos);
        model.addAttribute("totalConsumoPendiente", totalConsumoPendiente);
        model.addAttribute("utilidad", utilidad);
        return "reportes/index";
    }

    @GetMapping({"/pdf", "/ventas.pdf"})
    public ResponseEntity<byte[]> descargarPdfVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            byte[] pdfBytes = reporteService.generarPdfVentas(desde, hasta);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Reporte_Ventas_Chilpayate.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error al generar PDF de reporte de ventas", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/ventas-gastos.pdf")
    public ResponseEntity<byte[]> descargarPdfVentasGastos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            byte[] pdfBytes = reporteService.generarPdfVentasGastos(desde, hasta);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Reporte_Ventas_vs_Gastos_Chilpayate.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error al generar PDF de ventas vs gastos", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping({"/indicadores.pdf", "/kpi.pdf"})
    public ResponseEntity<byte[]> descargarPdfIndicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            byte[] pdfBytes = reporteService.generarPdfIndicadores(desde, hasta);
            String filename = "Reporte_KPI_Indicadores_Chilpayate_" + (desde != null ? desde : LocalDate.now()) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error al generar PDF de indicadores y KPIs en JasperReports", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
