package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.dto.ReporteAnaliticoDto;
import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final VentaRepository ventaRepo;
    private final GastoRepository gastoRepo;
    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final DetallePedidoRepository detallePedidoRepo;
    private final DetallePedidoSalsaRepository detallePedidoSalsaRepo;
    private final PagoMedioRepository pagoMedioRepo;
    private final ConfiguracionService configuracionService;

    @Autowired
    public ReporteService(VentaRepository ventaRepo,
                          GastoRepository gastoRepo,
                          PedidoRepository pedidoRepo,
                          ProductoRepository productoRepo,
                          DetallePedidoRepository detallePedidoRepo,
                          DetallePedidoSalsaRepository detallePedidoSalsaRepo,
                          PagoMedioRepository pagoMedioRepo,
                          ConfiguracionService configuracionService) {
        this.ventaRepo = ventaRepo;
        this.gastoRepo = gastoRepo;
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.detallePedidoRepo = detallePedidoRepo;
        this.detallePedidoSalsaRepo = detallePedidoSalsaRepo;
        this.pagoMedioRepo = pagoMedioRepo;
        this.configuracionService = configuracionService;
    }

    public ReporteService(VentaRepository ventaRepo,
                          GastoRepository gastoRepo,
                          PedidoRepository pedidoRepo,
                          ProductoRepository productoRepo,
                          DetallePedidoRepository detallePedidoRepo,
                          DetallePedidoSalsaRepository detallePedidoSalsaRepo,
                          PagoMedioRepository pagoMedioRepo) {
        this(ventaRepo, gastoRepo, pedidoRepo, productoRepo, detallePedidoRepo, detallePedidoSalsaRepo, pagoMedioRepo, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumenGeneral() {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);

        List<Venta> ventasHoy = ventaRepo.findByFechaHoraBetween(inicioHoy, finHoy);
        BigDecimal totalVentas = ventasHoy.stream()
                .filter(v -> Boolean.TRUE.equals(v.getEstado()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Gasto> gastosHoy = gastoRepo.findByFechaBetweenOrderByFechaDesc(LocalDate.now(), LocalDate.now());
        BigDecimal totalGastos = gastosHoy.stream()
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> map = new HashMap<>();
        map.put("totalVentasHoy", totalVentas);
        map.put("totalGastosHoy", totalGastos);
        map.put("balanceHoy", totalVentas.subtract(totalGastos));
        map.put("cantidadVentasHoy", ventasHoy.size());
        map.put("cantidadGastosHoy", gastosHoy.size());
        map.put("stockBajo", productoRepo.findStockBajo(10));

        return map;
    }

    @Transactional(readOnly = true)
    public List<Venta> listarVentasPorRango(LocalDate desde, LocalDate hasta) {
        if (desde == null)
            desde = LocalDate.now().minusDays(7);
        if (hasta == null)
            hasta = LocalDate.now();
        return ventaRepo.findByFechaHoraBetween(desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));
    }

    @Transactional(readOnly = true)
    public List<Gasto> listarGastosPorRango(LocalDate desde, LocalDate hasta) {
        if (desde == null)
            desde = LocalDate.now().minusDays(7);
        if (hasta == null)
            hasta = LocalDate.now();
        return gastoRepo.findByFechaBetweenOrderByFechaDesc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosPorRango(LocalDate desde, LocalDate hasta) {
        if (desde == null)
            desde = LocalDate.now().minusDays(7);
        if (hasta == null)
            hasta = LocalDate.now();
        return pedidoRepo.findByFechaBetweenOrderByFechaDesc(desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));
    }

    @Transactional(readOnly = true)
    public ReporteAnaliticoDto obtenerReporteAnalitico(LocalDate desde, LocalDate hasta) {
        if (desde == null) desde = LocalDate.now();
        if (hasta == null) hasta = LocalDate.now();

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);

        ReporteAnaliticoDto dto = new ReporteAnaliticoDto();

        // 1. Carga de datos base en el rango de fechas
        List<Pedido> pedidos = pedidoRepo.findByFechaBetweenOrderByFechaDesc(inicio, fin);
        List<Venta> ventas = ventaRepo.findByFechaHoraBetween(inicio, fin);
        List<Gasto> gastos = gastoRepo.findByFechaBetweenOrderByFechaDesc(desde, hasta);
        List<DetallePedido> detalles = detallePedidoRepo.findDetallesValidosEntreFechas(inicio, fin);
        List<DetallePedidoSalsa> salsas = detallePedidoSalsaRepo.findSalsasValidasEntreFechas(inicio, fin);
        List<PagoMedio> pagosMedios = pagoMedioRepo.findPagosMediosEntreFechas(inicio, fin);

        // Filtrar pedidos válidos (no anulados)
        List<Pedido> pedidosValidos = pedidos.stream()
                .filter(p -> p != null && p.getEstado() != Pedido.EstadoPedido.ANULADA)
                .toList();

        // Totales financieros
        BigDecimal totalVentasCobradas = ventas.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getEstado()) && v.getTotal() != null)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPedidosMonto = pedidosValidos.stream()
                .map(Pedido::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGastos = gastos.stream()
                .filter(g -> g != null && g.getMonto() != null)
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVentas = (totalVentasCobradas.compareTo(BigDecimal.ZERO) > 0)
                ? totalVentasCobradas
                : totalPedidosMonto;

        dto.setTotalVentas(totalVentas);
        dto.setTotalVentasCobradas(totalVentasCobradas);
        dto.setTotalGastos(totalGastos);
        dto.setUtilidadNeta(totalVentas.subtract(totalGastos));
        dto.setTotalComandas(pedidosValidos.size());
        dto.setTotalPlatosDespachados(detalles.size());

        if (!pedidosValidos.isEmpty()) {
            dto.setTicketPromedio(totalPedidosMonto.divide(BigDecimal.valueOf(pedidosValidos.size()), 2, RoundingMode.HALF_UP));
        }

        // 2. Ranking de Platos Más Vendidos
        Map<Integer, List<DetallePedido>> porProducto = detalles.stream()
                .filter(d -> d.getProducto() != null)
                .collect(Collectors.groupingBy(d -> d.getProducto().getIdProducto()));

        List<ReporteAnaliticoDto.PlatoVendidoDto> listaPlatos = new ArrayList<>();
        for (Map.Entry<Integer, List<DetallePedido>> entry : porProducto.entrySet()) {
            List<DetallePedido> list = entry.getValue();
            Producto prod = list.get(0).getProducto();
            long cant = list.size();
            BigDecimal monto = list.stream()
                    .map(DetallePedido::getPrecioActual)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String catNombre = (prod.getCategoria() != null) ? prod.getCategoria().getNombre() : "General";
            listaPlatos.add(new ReporteAnaliticoDto.PlatoVendidoDto(prod.getIdProducto(), prod.getNombre(), catNombre, cant, monto, 0.0));
        }

        listaPlatos.sort((a, b) -> Long.compare(b.getCantidad(), a.getCantidad()));
        long totalPlatosCount = detalles.size();
        for (ReporteAnaliticoDto.PlatoVendidoDto item : listaPlatos) {
            if (totalPlatosCount > 0) {
                item.setPorcentaje(Math.round(((double) item.getCantidad() / totalPlatosCount * 100.0) * 10.0) / 10.0);
            }
        }
        dto.setTopPlatos(listaPlatos.stream().limit(10).toList());

        if (!listaPlatos.isEmpty()) {
            dto.setPlatoMasVendido(listaPlatos.get(0).getNombre());
            dto.setPlatoMasVendidoCant(listaPlatos.get(0).getCantidad());
        }

        // 3. Especial de Alitas El Chilpayate
        List<DetallePedido> detallesAlitas = detalles.stream()
                .filter(d -> d.getProducto() != null && esProductoAlitas(d.getProducto()))
                .toList();

        Map<Integer, List<DetallePedido>> porAlita = detallesAlitas.stream()
                .collect(Collectors.groupingBy(d -> d.getProducto().getIdProducto()));

        List<ReporteAnaliticoDto.AlitaVendidoDto> listaAlitas = new ArrayList<>();
        long totalPorcionesAlitas = 0;
        long totalPiezasAlitas = 0;

        for (Map.Entry<Integer, List<DetallePedido>> entry : porAlita.entrySet()) {
            List<DetallePedido> list = entry.getValue();
            Producto prod = list.get(0).getProducto();
            long cant = list.size();
            long piezasPorPlato = extraerPiezasAlitas(prod.getNombre());
            long piezasTotales = cant * piezasPorPlato;

            BigDecimal monto = list.stream()
                    .map(DetallePedido::getPrecioActual)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalPorcionesAlitas += cant;
            totalPiezasAlitas += piezasTotales;

            listaAlitas.add(new ReporteAnaliticoDto.AlitaVendidoDto(prod.getIdProducto(), prod.getNombre(), cant, piezasTotales, monto, 0.0));
        }

        listaAlitas.sort((a, b) -> Long.compare(b.getCantidad(), a.getCantidad()));
        for (ReporteAnaliticoDto.AlitaVendidoDto item : listaAlitas) {
            if (totalPorcionesAlitas > 0) {
                item.setPorcentaje(Math.round(((double) item.getCantidad() / totalPorcionesAlitas * 100.0) * 10.0) / 10.0);
            }
        }
        dto.setTopAlitas(listaAlitas);
        dto.setTotalPorcionesAlitas(totalPorcionesAlitas);
        dto.setTotalPiezasAlitas(totalPiezasAlitas);

        if (!listaAlitas.isEmpty()) {
            dto.setAlitaMasVendida(listaAlitas.get(0).getNombre());
            dto.setAlitaMasVendidaCant(listaAlitas.get(0).getCantidad());
            dto.setAlitaMasVendidaPiezas(listaAlitas.get(0).getPiezasEstimadas());
        }

        // 4. Top Salsas Más Pedidas
        Map<String, Long> salsasCount = salsas.stream()
                .filter(s -> s.getSalsa() != null && s.getSalsa().getNombre() != null)
                .collect(Collectors.groupingBy(s -> s.getSalsa().getNombre(), Collectors.counting()));

        List<ReporteAnaliticoDto.SalsaTopDto> listaSalsas = new ArrayList<>();
        long totalSalsasPedidas = salsas.size();

        salsasCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    double pct = (totalSalsasPedidas > 0)
                            ? Math.round(((double) entry.getValue() / totalSalsasPedidas * 100.0) * 10.0) / 10.0
                            : 0.0;
                    listaSalsas.add(new ReporteAnaliticoDto.SalsaTopDto(0, entry.getKey(), entry.getValue(), pct));
                });

        dto.setTopSalsas(listaSalsas);
        if (!listaSalsas.isEmpty()) {
            dto.setSalsaMasPedida(listaSalsas.get(0).getNombre());
            dto.setSalsaMasPedidaCant(listaSalsas.get(0).getVecesPedida());
        }

        // 5. Rendimiento de Mozos / Personal
        Map<Integer, List<Pedido>> pedidosPorUsuario = pedidosValidos.stream()
                .filter(p -> p.getUsuario() != null)
                .collect(Collectors.groupingBy(p -> p.getUsuario().getIdUsuario()));

        List<ReporteAnaliticoDto.MozoRendimientoDto> listaMozos = new ArrayList<>();

        for (Map.Entry<Integer, List<Pedido>> entry : pedidosPorUsuario.entrySet()) {
            List<Pedido> pList = entry.getValue();
            Usuario u = pList.get(0).getUsuario();
            long countPedidos = pList.size();
            BigDecimal totalMonto = pList.stream()
                    .map(Pedido::getTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long platosVendidos = pList.stream()
                    .filter(p -> p.getDetallePedido() != null)
                    .mapToLong(p -> p.getDetallePedido().stream()
                            .filter(d -> d != null && d.getEstado() != DetallePedido.EstadoDetalle.ANULADO)
                            .count())
                    .sum();

            BigDecimal ticketPromedio = (countPedidos > 0)
                    ? totalMonto.divide(BigDecimal.valueOf(countPedidos), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String nombreCompleto = (u.getNombres() != null ? u.getNombres() : "") +
                    (u.getApellidos() != null ? " " + u.getApellidos() : "");
            if (nombreCompleto.isBlank()) nombreCompleto = u.getUsername();

            String rolNombre = (u.getRol() != null && u.getRol().getNombre() != null) ? u.getRol().getNombre() : "Personal";

            listaMozos.add(new ReporteAnaliticoDto.MozoRendimientoDto(
                    u.getIdUsuario(), nombreCompleto.trim(), u.getUsername(), rolNombre, countPedidos, platosVendidos, totalMonto, ticketPromedio
            ));
        }

        // Ordenar mozos por mayor cantidad de platos vendidos, luego por total en dinero
        listaMozos.sort((a, b) -> {
            int cmp = Long.compare(b.getPlatosVendidos(), a.getPlatosVendidos());
            if (cmp != 0) return cmp;
            return b.getTotalVendido().compareTo(a.getTotalVendido());
        });
        dto.setRankingMozos(listaMozos);

        if (!listaMozos.isEmpty()) {
            dto.setMozoEstrella(listaMozos.get(0).getNombreCompleto());
            dto.setMozoEstrellaPlatos(listaMozos.get(0).getPlatosVendidos());
            dto.setMozoEstrellaMonto(listaMozos.get(0).getTotalVendido());
        }

        // 6. Ventas por Canal (Salón vs Delivery)
        long salonCount = pedidosValidos.stream().filter(p -> p.getTipo() == Pedido.TipoPedido.SALON).count();
        BigDecimal salonMonto = pedidosValidos.stream()
                .filter(p -> p.getTipo() == Pedido.TipoPedido.SALON)
                .map(Pedido::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        long delCount = pedidosValidos.stream().filter(p -> p.getTipo() == Pedido.TipoPedido.DELIVERY).count();
        BigDecimal delMonto = pedidosValidos.stream()
                .filter(p -> p.getTipo() == Pedido.TipoPedido.DELIVERY)
                .map(Pedido::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalSumaCanales = salonMonto.add(delMonto).doubleValue();

        List<ReporteAnaliticoDto.CanalVentaDto> canales = new ArrayList<>();
        canales.add(new ReporteAnaliticoDto.CanalVentaDto(
                "SALON", "Salón (Mesas)", "▦", salonCount, salonMonto,
                (salonCount > 0 ? salonMonto.divide(BigDecimal.valueOf(salonCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO),
                (totalSumaCanales > 0 ? Math.round((salonMonto.doubleValue() / totalSumaCanales * 100.0) * 10.0) / 10.0 : 0.0)
        ));
        canales.add(new ReporteAnaliticoDto.CanalVentaDto(
                "DELIVERY", "Delivery / Llevar", "🛵", delCount, delMonto,
                (delCount > 0 ? delMonto.divide(BigDecimal.valueOf(delCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO),
                (totalSumaCanales > 0 ? Math.round((delMonto.doubleValue() / totalSumaCanales * 100.0) * 10.0) / 10.0 : 0.0)
        ));
        dto.setVentasPorCanal(canales);

        // 7. Mix de Métodos de Pago
        Map<String, BigDecimal> metodoMontos = new LinkedHashMap<>();
        Map<String, Long> metodoTransacc = new LinkedHashMap<>();

        if (!pagosMedios.isEmpty()) {
            for (PagoMedio pm : pagosMedios) {
                String metodo = (pm.getMetodoPago() != null && pm.getMetodoPago().getNombre() != null)
                        ? pm.getMetodoPago().getNombre() : "EFECTIVO";
                BigDecimal m = pm.getMonto() != null ? pm.getMonto() : BigDecimal.ZERO;
                metodoMontos.put(metodo, metodoMontos.getOrDefault(metodo, BigDecimal.ZERO).add(m));
                metodoTransacc.put(metodo, metodoTransacc.getOrDefault(metodo, 0L) + 1L);
            }
        } else {
            // Fallback a través de Venta si no hay PagoMedio registrado
            for (Venta v : ventas) {
                String m = "EFECTIVO";
                if (v.getPago() != null && v.getPago().getTipoPago() != null) {
                    m = v.getPago().getTipoPago().name();
                }
                BigDecimal tot = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                metodoMontos.put(m, metodoMontos.getOrDefault(m, BigDecimal.ZERO).add(tot));
                metodoTransacc.put(m, metodoTransacc.getOrDefault(m, 0L) + 1L);
            }
        }

        BigDecimal sumaMetodos = metodoMontos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReporteAnaliticoDto.MetodoPagoMixDto> mixPagos = new ArrayList<>();
        metodoMontos.forEach((metodo, monto) -> {
            double pct = (sumaMetodos.compareTo(BigDecimal.ZERO) > 0)
                    ? Math.round((monto.doubleValue() / sumaMetodos.doubleValue() * 100.0) * 10.0) / 10.0
                    : 0.0;
            mixPagos.add(new ReporteAnaliticoDto.MetodoPagoMixDto(metodo, monto, metodoTransacc.getOrDefault(metodo, 0L), pct));
        });
        mixPagos.sort((a, b) -> b.getTotalMonto().compareTo(a.getTotalMonto()));
        dto.setMixMetodosPago(mixPagos);

        // 8. Horarios Pico (Franjas Horarias)
        Map<Integer, List<Pedido>> porHora = pedidosValidos.stream()
                .filter(p -> p.getFecha() != null)
                .collect(Collectors.groupingBy(p -> p.getFecha().getHour()));

        List<ReporteAnaliticoDto.HorarioPicoDto> listaHoras = new ArrayList<>();
        long maxPedidosHora = 0;
        String mejorHoraLabel = "Sin pedidos";

        List<Integer> horasOrdenadas = new ArrayList<>(porHora.keySet());
        Collections.sort(horasOrdenadas);

        for (Integer hora : horasOrdenadas) {
            List<Pedido> pList = porHora.get(hora);
            long countH = pList.size();
            BigDecimal montoH = pList.stream().map(Pedido::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            String label = String.format("%02d:00 - %02d:00", hora, (hora + 1) % 24);

            double pct = (!pedidosValidos.isEmpty())
                    ? Math.round(((double) countH / pedidosValidos.size() * 100.0) * 10.0) / 10.0
                    : 0.0;

            listaHoras.add(new ReporteAnaliticoDto.HorarioPicoDto(hora, label, countH, montoH, pct));

            if (countH > maxPedidosHora) {
                maxPedidosHora = countH;
                mejorHoraLabel = label;
            }
        }

        dto.setHorasPunta(listaHoras);
        dto.setHoraPico(mejorHoraLabel);
        dto.setHoraPicoPedidos(maxPedidosHora);

        return dto;
    }

    private boolean esProductoAlitas(Producto p) {
        if (p == null) return false;
        if (p.getCategoria() != null && p.getCategoria().getNombre() != null &&
                p.getCategoria().getNombre().toLowerCase().contains("alita")) {
            return true;
        }
        return p.getNombre() != null && p.getNombre().toLowerCase().contains("alita");
    }

    private long extraerPiezasAlitas(String nombre) {
        if (nombre == null) return 6;
        Matcher m = Pattern.compile("(\\d+)").matcher(nombre);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (Exception ignored) {}
        }
        return 6;
    }

    public static class ReporteVentaItemDto {
        private Long id_venta;
        private java.sql.Timestamp fecha;
        private String numero_comprobante;
        private String tipo;
        private String tipo_comprobante;
        private BigDecimal total;

        public ReporteVentaItemDto() {}
        public ReporteVentaItemDto(Long id_venta, java.sql.Timestamp fecha, String numero_comprobante, String tipo, String tipo_comprobante, BigDecimal total) {
            this.id_venta = id_venta;
            this.fecha = fecha;
            this.numero_comprobante = numero_comprobante;
            this.tipo = tipo;
            this.tipo_comprobante = tipo_comprobante;
            this.total = total;
        }

        public Long getId_venta() { return id_venta; }
        public void setId_venta(Long id_venta) { this.id_venta = id_venta; }
        public java.sql.Timestamp getFecha() { return fecha; }
        public void setFecha(java.sql.Timestamp fecha) { this.fecha = fecha; }
        public String getNumero_comprobante() { return numero_comprobante; }
        public void setNumero_comprobante(String numero_comprobante) { this.numero_comprobante = numero_comprobante; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getTipo_comprobante() { return tipo_comprobante; }
        public void setTipo_comprobante(String tipo_comprobante) { this.tipo_comprobante = tipo_comprobante; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
    }

    public static class ReporteConceptoMontoDto {
        private String concepto;
        private BigDecimal monto;

        public ReporteConceptoMontoDto() {}
        public ReporteConceptoMontoDto(String concepto, BigDecimal monto) {
            this.concepto = concepto;
            this.monto = monto;
        }

        public String getConcepto() { return concepto; }
        public void setConcepto(String concepto) { this.concepto = concepto; }
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfVentas(LocalDate desde, LocalDate hasta) throws Exception {
        if (desde == null) desde = LocalDate.now().minusDays(7);
        if (hasta == null) hasta = LocalDate.now();

        List<Venta> ventas = listarVentasPorRango(desde, hasta);
        List<ReporteVentaItemDto> items = new ArrayList<>();
        for (Venta v : ventas) {
            Long id = v.getIdVenta() != null ? v.getIdVenta().longValue() : 0L;
            java.sql.Timestamp ts = v.getFechaHora() != null ? java.sql.Timestamp.valueOf(v.getFechaHora()) : new java.sql.Timestamp(System.currentTimeMillis());
            String comp = v.getNumeroComprobante() != null ? v.getNumeroComprobante() : "-";
            String tipo = (v.getPedido() != null && v.getPedido().getTipo() != null) ? v.getPedido().getTipo().name() : "SALON";
            String tipoComp = v.getTipoComprobante() != null ? v.getTipoComprobante() : "BOLETA";
            BigDecimal total = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;

            items.add(new ReporteVentaItemDto(id, ts, comp, tipo, tipoComp, total));
        }

        // Si no hay ventas con comprobante en la tabla pero sí comandas/pedidos registrados
        if (items.isEmpty()) {
            List<Pedido> pedidos = listarPedidosPorRango(desde, hasta);
            for (Pedido p : pedidos) {
                if (p != null && p.getEstado() != Pedido.EstadoPedido.ANULADA) {
                    Long id = p.getIdPedido() != null ? p.getIdPedido().longValue() : 0L;
                    java.sql.Timestamp ts = p.getFecha() != null ? java.sql.Timestamp.valueOf(p.getFecha()) : new java.sql.Timestamp(System.currentTimeMillis());
                    String comp = p.getCodigo() != null ? p.getCodigo() : "PED-" + id;
                    String tipo = (p.getTipo() != null) ? p.getTipo().name() : "SALON";
                    String tipoComp = (p.getEstado() != null) ? p.getEstado().name() : "COMANDA";
                    BigDecimal total = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;

                    items.add(new ReporteVentaItemDto(id, ts, comp, tipo, tipoComp, total));
                }
            }
        }

        java.io.InputStream stream = getClass().getResourceAsStream("/reports/ventas.jrxml");
        if (stream == null) {
            throw new IllegalStateException("Plantilla de reporte ventas.jrxml no encontrada");
        }

        net.sf.jasperreports.engine.JasperReport jasperReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(stream);
        Map<String, Object> params = new HashMap<>();
        params.put("DESDE", java.sql.Date.valueOf(desde));
        params.put("HASTA", java.sql.Date.valueOf(hasta));

        net.sf.jasperreports.engine.data.JRBeanCollectionDataSource ds = new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(items);
        net.sf.jasperreports.engine.JasperPrint print = net.sf.jasperreports.engine.JasperFillManager.fillReport(jasperReport, params, ds);

        return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(print);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfVentasGastos(LocalDate desde, LocalDate hasta) throws Exception {
        if (desde == null) desde = LocalDate.now().minusDays(7);
        if (hasta == null) hasta = LocalDate.now();

        List<Venta> ventas = listarVentasPorRango(desde, hasta);
        BigDecimal totalVentas = ventas.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getEstado()) && v.getTotal() != null)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalVentas.compareTo(BigDecimal.ZERO) == 0) {
            List<Pedido> pedidos = listarPedidosPorRango(desde, hasta);
            totalVentas = pedidos.stream()
                    .filter(p -> p != null && p.getEstado() != Pedido.EstadoPedido.ANULADA && p.getTotal() != null)
                    .map(Pedido::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<Gasto> gastos = listarGastosPorRango(desde, hasta);
        BigDecimal totalGastos = gastos.stream()
                .filter(g -> g != null && g.getMonto() != null)
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaNeta = totalVentas.subtract(totalGastos);

        List<ReporteConceptoMontoDto> items = new ArrayList<>();
        items.add(new ReporteConceptoMontoDto("VENTAS REGISTRADAS", totalVentas));
        items.add(new ReporteConceptoMontoDto("GASTOS OPERATIVOS", totalGastos));
        items.add(new ReporteConceptoMontoDto("UTILIDAD NETA", gananciaNeta));

        java.io.InputStream stream = getClass().getResourceAsStream("/reports/ventas_gastos.jrxml");
        if (stream == null) {
            throw new IllegalStateException("Plantilla de reporte ventas_gastos.jrxml no encontrada");
        }

        net.sf.jasperreports.engine.JasperReport jasperReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(stream);
        Map<String, Object> params = new HashMap<>();
        params.put("DESDE", java.sql.Date.valueOf(desde));
        params.put("HASTA", java.sql.Date.valueOf(hasta));

        net.sf.jasperreports.engine.data.JRBeanCollectionDataSource ds = new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(items);
        net.sf.jasperreports.engine.JasperPrint print = net.sf.jasperreports.engine.JasperFillManager.fillReport(jasperReport, params, ds);

        return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(print);
    }

    public static class ReporteIndicadorRowDto {
        private String seccion;
        private Integer orden;
        private String item;
        private String detalle;
        private Long cantidad;
        private BigDecimal monto;
        private Double porcentaje;

        public ReporteIndicadorRowDto() {}

        public ReporteIndicadorRowDto(String seccion, Integer orden, String item, String detalle, Long cantidad, BigDecimal monto, Double porcentaje) {
            this.seccion = seccion;
            this.orden = orden;
            this.item = item;
            this.detalle = detalle;
            this.cantidad = cantidad;
            this.monto = monto;
            this.porcentaje = porcentaje;
        }

        public String getSeccion() { return seccion; }
        public void setSeccion(String seccion) { this.seccion = seccion; }
        public Integer getOrden() { return orden; }
        public void setOrden(Integer orden) { this.orden = orden; }
        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }
        public String getDetalle() { return detalle; }
        public void setDetalle(String detalle) { this.detalle = detalle; }
        public Long getCantidad() { return cantidad; }
        public void setCantidad(Long cantidad) { this.cantidad = cantidad; }
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
        public Double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(Double porcentaje) { this.porcentaje = porcentaje; }
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfIndicadores(LocalDate desde, LocalDate hasta) throws Exception {
        if (desde == null) desde = LocalDate.now().minusDays(7);
        if (hasta == null) hasta = LocalDate.now();

        ReporteAnaliticoDto analitica = obtenerReporteAnalitico(desde, hasta);
        Configuracion conf = configuracionService != null ? configuracionService.obtenerConfiguracion() : null;

        List<ReporteIndicadorRowDto> items = new ArrayList<>();

        // 1. Especial de Alitas
        if (analitica.getTopAlitas() != null && !analitica.getTopAlitas().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.AlitaVendidoDto a : analitica.getTopAlitas()) {
                items.add(new ReporteIndicadorRowDto(
                        "1. ESPECIAL DE ALITAS (VENTAS POR PRESENTACIÓN)",
                        orden++,
                        a.getNombre(),
                        "~" + a.getPiezasEstimadas() + " piezas estimadas",
                        a.getCantidad(),
                        a.getRecaudacion(),
                        a.getPorcentaje()
                ));
            }
        }

        // 2. Salsas de la casa
        if (analitica.getTopSalsas() != null && !analitica.getTopSalsas().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.SalsaTopDto s : analitica.getTopSalsas()) {
                items.add(new ReporteIndicadorRowDto(
                        "2. SALSAS ARTESANALES DE LA CASA MÁS PEDIDAS",
                        orden++,
                        s.getNombre(),
                        "Salsa artesanal para alitas",
                        s.getVecesPedida(),
                        BigDecimal.ZERO,
                        s.getPorcentaje()
                ));
            }
        }

        // 3. Rendimiento de Mozos / Personal
        if (analitica.getRankingMozos() != null && !analitica.getRankingMozos().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.MozoRendimientoDto m : analitica.getRankingMozos()) {
                items.add(new ReporteIndicadorRowDto(
                        "3. PRODUCTIVIDAD Y RANKING DE MOZOS / PERSONAL",
                        orden++,
                        m.getNombreCompleto(),
                        m.getRol() + " (" + m.getPedidosAtendidos() + " com.) | T.Prom: S/ " + m.getTicketPromedio(),
                        m.getPlatosVendidos(),
                        m.getTotalVendido(),
                        null
                ));
            }
        }

        // 4. Top 10 Platos (Carta General)
        if (analitica.getTopPlatos() != null && !analitica.getTopPlatos().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.PlatoVendidoDto p : analitica.getTopPlatos()) {
                items.add(new ReporteIndicadorRowDto(
                        "4. TOP 10 PLATOS MÁS VENDIDOS (CARTA GENERAL)",
                        orden++,
                        p.getNombre(),
                        "Cat: " + p.getCategoria(),
                        p.getCantidad(),
                        p.getRecaudacion(),
                        p.getPorcentaje()
                ));
            }
        }

        // 5. Ventas por Canal
        if (analitica.getVentasPorCanal() != null && !analitica.getVentasPorCanal().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.CanalVentaDto c : analitica.getVentasPorCanal()) {
                items.add(new ReporteIndicadorRowDto(
                        "5. VENTAS POR CANAL (SALÓN VS. DELIVERY)",
                        orden++,
                        c.getCanalLabel(),
                        c.getCantidadPedidos() + " comandas | Ticket Prom: S/ " + c.getTicketPromedio(),
                        c.getCantidadPedidos(),
                        c.getTotalMonto(),
                        c.getPorcentaje()
                ));
            }
        }

        // 6. Mix de Medios de Pago
        if (analitica.getMixMetodosPago() != null && !analitica.getMixMetodosPago().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.MetodoPagoMixDto mp : analitica.getMixMetodosPago()) {
                items.add(new ReporteIndicadorRowDto(
                        "6. MIX DE MEDIOS DE PAGO",
                        orden++,
                        mp.getMetodo(),
                        mp.getTransacciones() + " transacciones de cobro",
                        mp.getTransacciones(),
                        mp.getTotalMonto(),
                        mp.getPorcentaje()
                ));
            }
        }

        // 7. Curva Horaria
        if (analitica.getHorasPunta() != null && !analitica.getHorasPunta().isEmpty()) {
            int orden = 1;
            for (ReporteAnaliticoDto.HorarioPicoDto h : analitica.getHorasPunta()) {
                boolean esPico = h.getRangoHorario().equals(analitica.getHoraPico());
                items.add(new ReporteIndicadorRowDto(
                        "7. CURVA DE DEMANDA HORARIA (FLUJO DE COCINA)",
                        orden++,
                        h.getRangoHorario(),
                        esPico ? "PICO MÁXIMO DE DEMANDA" : "Flujo estándar",
                        h.getCantidadPedidos(),
                        h.getTotalMonto(),
                        h.getPorcentaje()
                ));
            }
        }

        if (items.isEmpty()) {
            items.add(new ReporteIndicadorRowDto(
                    "RESUMEN OPERATIVO",
                    1,
                    "Sin operaciones registradas",
                    "No se encontraron ventas ni pedidos en este rango",
                    0L,
                    BigDecimal.ZERO,
                    0.0
            ));
        }

        java.io.InputStream stream = getClass().getResourceAsStream("/reports/indicadores_kpi.jrxml");
        if (stream == null) {
            throw new IllegalStateException("Plantilla de reporte indicadores_kpi.jrxml no encontrada");
        }

        net.sf.jasperreports.engine.JasperReport jasperReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(stream);
        Map<String, Object> params = new HashMap<>();
        params.put("DESDE", java.sql.Date.valueOf(desde));
        params.put("HASTA", java.sql.Date.valueOf(hasta));

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        params.put("FECHA_EMISION", LocalDateTime.now().format(dtf));

        String nombreEmpresa = (conf != null && conf.getNombre() != null) ? conf.getNombre() : ConfiguracionService.DEFAULT_NOMBRE;
        String rucEmpresa = (conf != null && conf.getRuc() != null) ? conf.getRuc() : ConfiguracionService.DEFAULT_RUC;
        String dirEmpresa = (conf != null && conf.getDireccion() != null) ? conf.getDireccion() : ConfiguracionService.DEFAULT_DIRECCION;
        if (conf != null && conf.getDistrito() != null) dirEmpresa += " - " + conf.getDistrito();

        params.put("EMPRESA_NOMBRE", nombreEmpresa);
        params.put("EMPRESA_RUC", rucEmpresa);
        params.put("EMPRESA_DIRECCION", dirEmpresa);

        params.put("TOTAL_VENTAS", analitica.getTotalVentas() != null ? analitica.getTotalVentas() : BigDecimal.ZERO);
        params.put("TICKET_PROMEDIO", analitica.getTicketPromedio() != null ? analitica.getTicketPromedio() : BigDecimal.ZERO);
        params.put("TOTAL_COMANDAS", analitica.getTotalComandas());
        params.put("TOTAL_PLATOS", analitica.getTotalPlatosDespachados());
        params.put("TOTAL_ALITAS_PIEZAS", analitica.getTotalPiezasAlitas());
        params.put("TOTAL_ALITAS_PORCIONES", analitica.getTotalPorcionesAlitas());
        params.put("UTILIDAD_NETA", analitica.getUtilidadNeta() != null ? analitica.getUtilidadNeta() : BigDecimal.ZERO);

        params.put("ALITA_ESTRELLA", analitica.getAlitaMasVendida() != null ? analitica.getAlitaMasVendida() : "Sin registro");
        params.put("SALSA_ESTRELLA", analitica.getSalsaMasPedida() != null ? analitica.getSalsaMasPedida() : "Sin registro");
        params.put("MOZO_ESTRELLA", analitica.getMozoEstrella() != null ? analitica.getMozoEstrella() : "Sin registro");
        params.put("HORA_PUNTA", analitica.getHoraPico() != null ? analitica.getHoraPico() : "Sin registro");

        net.sf.jasperreports.engine.data.JRBeanCollectionDataSource ds = new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(items);
        net.sf.jasperreports.engine.JasperPrint print = net.sf.jasperreports.engine.JasperFillManager.fillReport(jasperReport, params, ds);

        return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(print);
    }
}
