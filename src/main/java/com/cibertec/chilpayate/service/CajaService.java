package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private final PagoRepository pagoRepo;
    private final PagoMedioRepository pagoMedioRepo;
    private final PagoDetalleRepository pagoDetalleRepo;
    private final VentaRepository ventaRepo;
    private final PedidoRepository pedidoRepo;
    private final MesaRepository mesaRepo;
    private final MetodoPagoRepository metodoPagoRepo;
    private final CierreCajaRepository cierreRepo;
    private final GastoRepository gastoRepo;
    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepo;
    private final PrecuentaRepository precuentaRepo;
    private final PrecuentaDetalleRepository precuentaDetalleRepo;
    private final PrecuentaService precuentaService;
    private final DespachoService despachoService;

    public CajaService(PagoRepository pagoRepo,
                       PagoMedioRepository pagoMedioRepo,
                       PagoDetalleRepository pagoDetalleRepo,
                       VentaRepository ventaRepo,
                       PedidoRepository pedidoRepo,
                       MesaRepository mesaRepo,
                       MetodoPagoRepository metodoPagoRepo,
                       CierreCajaRepository cierreRepo,
                       GastoRepository gastoRepo,
                       PedidoService pedidoService,
                       UsuarioRepository usuarioRepo,
                       PrecuentaRepository precuentaRepo,
                       PrecuentaDetalleRepository precuentaDetalleRepo,
                       PrecuentaService precuentaService,
                       DespachoService despachoService) {
        this.pagoRepo = pagoRepo;
        this.pagoMedioRepo = pagoMedioRepo;
        this.pagoDetalleRepo = pagoDetalleRepo;
        this.ventaRepo = ventaRepo;
        this.pedidoRepo = pedidoRepo;
        this.mesaRepo = mesaRepo;
        this.metodoPagoRepo = metodoPagoRepo;
        this.cierreRepo = cierreRepo;
        this.gastoRepo = gastoRepo;
        this.pedidoService = pedidoService;
        this.usuarioRepo = usuarioRepo;
        this.precuentaRepo = precuentaRepo;
        this.precuentaDetalleRepo = precuentaDetalleRepo;
        this.precuentaService = precuentaService;
        this.despachoService = despachoService;
    }

    @org.springframework.cache.annotation.Cacheable(value = "metodosPago")
    @Transactional(readOnly = true)
    public List<MetodoPago> listarMetodosPago() {
        return metodoPagoRepo.findByEstadoTrue();
    }

    public List<Venta> listarVentasHoy() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return ventaRepo.findByFechaHoraBetween(inicio, fin);
    }

    public List<CierreCaja> listarCierres() {
        return cierreRepo.findAllByOrderByFechaAperturaDesc();
    }

    public Optional<CierreCaja> obtenerCajaAbierta() {
        return cierreRepo.findTopByOrderByFechaAperturaDesc()
                .filter(c -> c.getFechaCierre() == null);
    }

    @Transactional
    public CierreCaja abrirCaja(Usuario usuario, BigDecimal montoApertura) {
        if (obtenerCajaAbierta().isPresent()) {
            throw new IllegalStateException("Ya existe una caja abierta en el turno actual");
        }
        Usuario managedUser = null;
        if (usuario != null && usuario.getIdUsuario() != null) {
            managedUser = usuarioRepo.findById(usuario.getIdUsuario()).orElse(usuario);
        }

        CierreCaja c = new CierreCaja();
        c.setUsuario(managedUser);
        c.setFechaApertura(LocalDateTime.now());
        c.setMontoApertura(montoApertura != null ? montoApertura : BigDecimal.ZERO);
        c.setTotalVenta(BigDecimal.ZERO);
        c.setTotalGasto(BigDecimal.ZERO);
        c.setDiferencia(BigDecimal.ZERO);
        return cierreRepo.save(c);
    }

    public static class PagoItemDto {
        private Integer idMetodoPago;
        private BigDecimal monto;
        private String numeroOperacion;

        public PagoItemDto() {
        }

        public PagoItemDto(Integer idMetodoPago, BigDecimal monto, String numeroOperacion) {
            this.idMetodoPago = idMetodoPago;
            this.monto = monto;
            this.numeroOperacion = numeroOperacion;
        }

        public Integer getIdMetodoPago() {
            return idMetodoPago;
        }

        public void setIdMetodoPago(Integer idMetodoPago) {
            this.idMetodoPago = idMetodoPago;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public void setMonto(BigDecimal monto) {
            this.monto = monto;
        }

        public String getNumeroOperacion() {
            return numeroOperacion;
        }

        public void setNumeroOperacion(String numeroOperacion) {
            this.numeroOperacion = numeroOperacion;
        }
    }

    @Transactional
    public Venta procesarPagoTotal(Integer idPedido, Usuario usuario, Integer idMetodoPago,
            String numeroOperacion, String tipoComprobante,
            String numeroComprobante) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));
        BigDecimal total = pedidoService.calcularTotal(pedido);
        List<PagoItemDto> medios = List.of(new PagoItemDto(idMetodoPago, total, numeroOperacion));
        return procesarPagoMulti(idPedido, usuario, medios, tipoComprobante, numeroComprobante);
    }

    @Transactional
    public Venta procesarPagoPrecuenta(Integer idPrecuenta, Usuario usuario, List<PagoItemDto> medios,
            String tipoComprobante, String numeroComprobante, String nombreCliente, String dniRuc) {
        Precuenta precuenta = precuentaRepo.findById(idPrecuenta)
                .orElseThrow(() -> new IllegalArgumentException("Precuenta no encontrada: " + idPrecuenta));

        if (precuenta.getEstado() == Precuenta.EstadoPrecuenta.PAGADA) {
            throw new IllegalStateException(
                    "Esta precuenta (#" + precuenta.getNumeroPrecuenta() + ") ya fue cobrada previamente.");
        }

        Pedido pedido = precuenta.getPedido();

        // Regla de Negocio: Mientras no termine de despachar todos los pedidos o de clic en servido/entregado a mesa,
        // el terminal de caja no puede cobrar
        if (!despachoService.estaTotalmenteDespachado(pedido.getIdPedido())) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(pedido.getIdPedido());
            throw new IllegalStateException("No se puede cobrar la precuenta: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de procesar el pago.");
        }

        BigDecimal total = precuenta.getTotal();

        // 1. Crear Pago
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setPrecuenta(precuenta);
        pago.setTipoPago((medios != null && medios.size() > 1) ? Pago.TipoPago.SPLIT : Pago.TipoPago.UNICO);
        String docPago = (numeroComprobante != null && !numeroComprobante.isBlank())
                ? numeroComprobante.trim()
                : "PAG-" + System.currentTimeMillis() + "-" + ((int) (Math.random() * 900) + 100);
        pago.setCodigoDocumento(docPago);
        pago.setMontoTotal(total);
        pago.setFechaHora(LocalDateTime.now());
        Pago pagoGuardado = pagoRepo.save(pago);

        // 2. Registrar los medios de pago
        registrarMediosPago(pagoGuardado, medios, total);

        // 3. Vincular los detalles de pedido de la precuenta a PagoDetalle mediante insert nativo directo
        List<PrecuentaDetalle> detalles = precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(idPrecuenta);
        for (PrecuentaDetalle pd : detalles) {
            DetallePedido det = pd.getDetallePedido();
            if (det != null && det.getEstado() != DetallePedido.EstadoDetalle.ANULADO) {
                pagoDetalleRepo.insertarPagoDetalle(pagoGuardado.getIdPago(), det.getIdDetallePedido());
            }
        }

        // 4. Resolver Usuario de manera ultra-segura
        Usuario managedUsuario = resolverUsuarioSeguro(usuario, pedido);

        // Determinar datos del cliente y comprobante (Requisito 6: cliente y DNI/RUC opcionales y nulos por defecto)
        String clienteFinal = (nombreCliente != null && !nombreCliente.isBlank())
                ? nombreCliente.trim() : null;
        String dniRucFinal = (dniRuc != null && !dniRuc.isBlank())
                ? dniRuc.trim() : null;
        String tipoCompFinal = (tipoComprobante != null && !tipoComprobante.isBlank())
                ? tipoComprobante.trim() : "BOLETA";

        // 5. Emitir Venta / Comprobante (1 Pago -> 1 Venta con Precuenta y Cliente)
        Venta venta = new Venta();
        venta.setPedido(pedido);
        venta.setPago(pagoGuardado);
        venta.setPrecuenta(precuenta);
        venta.setUsuario(managedUsuario);
        venta.setTipoComprobante(tipoCompFinal);
        String compVenta = (numeroComprobante != null && !numeroComprobante.isBlank())
                ? numeroComprobante.trim()
                : (tipoCompFinal.startsWith("FACT") ? "F001-" : "B001-") + (System.currentTimeMillis() % 1000000);
        venta.setNumeroComprobante(compVenta);
        venta.setNombreCliente(clienteFinal);
        venta.setDniRuc(dniRucFinal);
        venta.setTotal(total);
        venta.setFechaHora(LocalDateTime.now());
        venta.setEstado(true);
        Venta ventaGuardada = ventaRepo.save(venta);

        // 6. Marcar precuenta como PAGADA y asociarla al pago
        precuenta.setPago(pagoGuardado);
        precuenta.setEstado(Precuenta.EstadoPrecuenta.PAGADA);
        precuentaRepo.save(precuenta);

        // 7. Evaluar si toda la comanda quedó totalmente pagada
        verificarYFinalizarPedidoSiCorresponde(pedido);

        return ventaGuardada;
    }

    @Transactional
    public Venta procesarPagoPrecuenta(Integer idPrecuenta, Usuario usuario, List<PagoItemDto> medios,
            String tipoComprobante, String numeroComprobante) {
        return procesarPagoPrecuenta(idPrecuenta, usuario, medios, tipoComprobante, numeroComprobante, null, null);
    }

    @Transactional
    public Venta procesarPagoMulti(Integer idPedido, Usuario usuario, List<PagoItemDto> medios,
            String tipoComprobante, String numeroComprobante) {
        return procesarPagoMulti(idPedido, usuario, medios, tipoComprobante, numeroComprobante, null, null);
    }

    @Transactional
    public Venta procesarPagoMulti(Integer idPedido, Usuario usuario, List<PagoItemDto> medios,
            String tipoComprobante, String numeroComprobante, String nombreCliente, String dniRuc) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));

        boolean esDelivery = (pedido.getTipo() == Pedido.TipoPedido.DELIVERY);

        // Regla de Negocio: Mientras no termine de despachar todos los pedidos o de clic en servido/entregado a mesa,
        // el terminal de caja no puede cobrar (SOLO PARA SALÓN)
        if (!esDelivery && !despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            throw new IllegalStateException("No se puede cobrar la comanda: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de procesar el pago.");
        }

        // Regla de Negocio: Debe haberse generado al menos una precuenta antes de cobrar (SOLO PARA SALÓN)
        List<Precuenta> precuentasPrevias = precuentaRepo.findByPedido_IdPedidoAndEstadoNot(idPedido, Precuenta.EstadoPrecuenta.ANULADA);
        if (!esDelivery && precuentasPrevias.isEmpty()) {
            throw new IllegalStateException("No se puede cobrar: Primero se debe generar la precuenta o precuentas divididas de la comanda.");
        }

        BigDecimal total = pedidoService.calcularTotal(pedido);

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setTipoPago((medios != null && medios.size() > 1) ? Pago.TipoPago.SPLIT : Pago.TipoPago.UNICO);
        String docPago = (numeroComprobante != null && !numeroComprobante.isBlank())
                ? numeroComprobante.trim()
                : "PAG-" + System.currentTimeMillis() + "-" + ((int) (Math.random() * 900) + 100);
        pago.setCodigoDocumento(docPago);
        pago.setMontoTotal(total);
        pago.setFechaHora(LocalDateTime.now());
        Pago pagoGuardado = pagoRepo.save(pago);

        // Registrar los medios de pago
        registrarMediosPago(pagoGuardado, medios, total);

        // Vincular todos los detalles de pedido al pago en PagoDetalle mediante insert nativo directo
        List<DetallePedido> detallesParaPago = pedido.getDetallePedido();
        if (detallesParaPago != null) {
            for (DetallePedido det : detallesParaPago) {
                if (det.getEstado() != DetallePedido.EstadoDetalle.ANULADO) {
                    pagoDetalleRepo.insertarPagoDetalle(pagoGuardado.getIdPago(), det.getIdDetallePedido());
                }
            }
        }

        Usuario managedUsuario = resolverUsuarioSeguro(usuario, pedido);

        String clienteFinal = (nombreCliente != null && !nombreCliente.isBlank())
                ? nombreCliente.trim()
                : (pedido.getCliente() != null ? pedido.getCliente().getNombre() : null);
        String dniRucFinal = (dniRuc != null && !dniRuc.isBlank())
                ? dniRuc.trim()
                : null;
        String tipoCompFinal = (tipoComprobante != null && !tipoComprobante.isBlank()) ? tipoComprobante : "BOLETA";

        // Emitir comprobante / venta
        Venta venta = new Venta();
        venta.setPedido(pedido);
        venta.setPago(pagoGuardado);
        venta.setUsuario(managedUsuario);
        venta.setTipoComprobante(tipoCompFinal);
        String compVenta = (numeroComprobante != null && !numeroComprobante.isBlank())
                ? numeroComprobante.trim()
                : (tipoCompFinal.startsWith("FACT") ? "F001-" : "B001-") + (System.currentTimeMillis() % 1000000);
        venta.setNumeroComprobante(compVenta);
        venta.setNombreCliente(clienteFinal);
        venta.setDniRuc(dniRucFinal);
        venta.setTotal(total);
        venta.setFechaHora(LocalDateTime.now());
        venta.setEstado(true);
        Venta ventaGuardada = ventaRepo.save(venta);

        // Marcar todas las precuentas de este pedido como pagadas
        List<Precuenta> precuentas = precuentaRepo.findByPedido_IdPedidoOrderByNumeroPrecuentaAsc(idPedido);
        if (precuentas.isEmpty()) {
            // Si no existían precuentas previas (ej: delivery), crear una precuenta consolidada pagada
            // para trazabilidad y vinculación con la venta
            Precuenta consolidada = new Precuenta();
            consolidada.setPedido(pedido);
            consolidada.setPago(pagoGuardado);
            consolidada.setNumeroPrecuenta(1);
            consolidada.setCodigo("PREC-" + pedido.getCodigo() + "-1");
            consolidada.setSubtotal(total.divide(new BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP));
            consolidada.setIgv(total.subtract(consolidada.getSubtotal()));
            consolidada.setTotal(total);
            consolidada.setFechaHora(LocalDateTime.now());
            consolidada.setEstado(Precuenta.EstadoPrecuenta.PAGADA);
            Precuenta precGuardada = precuentaRepo.save(consolidada);
            ventaGuardada.setPrecuenta(precGuardada);
            ventaRepo.save(ventaGuardada);
        } else {
            for (Precuenta prec : precuentas) {
                if (prec.getEstado() == Precuenta.EstadoPrecuenta.PENDIENTE) {
                    prec.setEstado(Precuenta.EstadoPrecuenta.PAGADA);
                    prec.setPago(pagoGuardado);
                    precuentaRepo.save(prec);
                }
            }
        }

        // Actualizar pedido a PAGADA y liberar mesa
        pedido.setEstado(Pedido.EstadoPedido.PAGADA);
        pedido.setHoraFinal(LocalDateTime.now());
        if (pedido.getMesa() != null) {
            Mesa mesa = pedido.getMesa();
            mesa.setEstado(Mesa.EstadoMesa.LIBRE);
            mesaRepo.save(mesa);
        }
        pedidoRepo.save(pedido);

        return ventaGuardada;
    }

    private void registrarMediosPago(Pago pago, List<PagoItemDto> medios, BigDecimal total) {
        if (medios != null && !medios.isEmpty()) {
            boolean agregoAlguno = false;
            for (PagoItemDto item : medios) {
                if (item.getMonto() == null || item.getMonto().compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                MetodoPago mp = null;
                if (item.getIdMetodoPago() != null) {
                    mp = metodoPagoRepo.findById(item.getIdMetodoPago()).orElse(null);
                }
                if (mp == null) {
                    mp = metodoPagoRepo.findAll().stream().findFirst().orElse(null);
                }

                if (mp != null) {
                    PagoMedio medio = new PagoMedio();
                    medio.setPago(pago);
                    medio.setMetodoPago(mp);
                    medio.setMonto(item.getMonto());
                    medio.setNumeroOperacion(item.getNumeroOperacion());
                    pagoMedioRepo.save(medio);
                    agregoAlguno = true;
                }
            }
            if (agregoAlguno)
                return;
        }

        // Si no se proporcionaron montos válidos en medios, registrar el total con el
        // primer método (Efectivo)
        MetodoPago mpDefault = metodoPagoRepo.findAll().stream().findFirst().orElse(null);
        if (mpDefault != null) {
            PagoMedio medio = new PagoMedio();
            medio.setPago(pago);
            medio.setMetodoPago(mpDefault);
            medio.setMonto(total);
            pagoMedioRepo.save(medio);
        }
    }

    private Usuario resolverUsuarioSeguro(Usuario usuario, Pedido pedido) {
        if (usuario != null && usuario.getIdUsuario() != null) {
            Optional<Usuario> u = usuarioRepo.findById(usuario.getIdUsuario());
            if (u.isPresent())
                return u.get();
        }
        if (pedido != null && pedido.getUsuario() != null) {
            return pedido.getUsuario();
        }
        return usuarioRepo.findAll().stream().findFirst().orElse(null);
    }

    private void verificarYFinalizarPedidoSiCorresponde(Pedido pedido) {
        // Verificar si quedan precuentas pendientes
        List<Precuenta> pendientes = precuentaRepo.findByPedido_IdPedidoAndEstado(pedido.getIdPedido(),
                Precuenta.EstadoPrecuenta.PENDIENTE);
        if (!pendientes.isEmpty()) {
            return; // Aún hay precuentas pendientes de pago
        }

        // Verificar si quedan ítems del pedido sin asignar ni pagar
        List<DetallePedido> disponibles = precuentaService.obtenerItemsDisponiblesParaDividir(pedido.getIdPedido());
        if (!disponibles.isEmpty()) {
            return; // Aún hay productos que no han sido incluidos en ninguna precuenta
        }

        // Si todas las precuentas están PAGADAS y no quedan ítems por cobrar:
        pedido.setEstado(Pedido.EstadoPedido.PAGADA);
        pedido.setHoraFinal(LocalDateTime.now());
        if (pedido.getMesa() != null) {
            Mesa mesa = pedido.getMesa();
            mesa.setEstado(Mesa.EstadoMesa.LIBRE);
            mesaRepo.save(mesa);
        }
        pedidoRepo.save(pedido);
    }

    public static class ResumenTurnoCajaDto {
        private BigDecimal montoApertura = BigDecimal.ZERO;
        private BigDecimal totalVentas = BigDecimal.ZERO;
        private BigDecimal ventasEfectivo = BigDecimal.ZERO;
        private BigDecimal ventasDigitales = BigDecimal.ZERO;
        private BigDecimal totalGastos = BigDecimal.ZERO;
        private BigDecimal efectivoEsperado = BigDecimal.ZERO;
        private int cantidadVentas = 0;

        public BigDecimal getMontoApertura() {
            return montoApertura;
        }

        public void setMontoApertura(BigDecimal montoApertura) {
            this.montoApertura = montoApertura;
        }

        public BigDecimal getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(BigDecimal totalVentas) {
            this.totalVentas = totalVentas;
        }

        public BigDecimal getVentasEfectivo() {
            return ventasEfectivo;
        }

        public void setVentasEfectivo(BigDecimal ventasEfectivo) {
            this.ventasEfectivo = ventasEfectivo;
        }

        public BigDecimal getVentasDigitales() {
            return ventasDigitales;
        }

        public void setVentasDigitales(BigDecimal ventasDigitales) {
            this.ventasDigitales = ventasDigitales;
        }

        public BigDecimal getTotalGastos() {
            return totalGastos;
        }

        public void setTotalGastos(BigDecimal totalGastos) {
            this.totalGastos = totalGastos;
        }

        public BigDecimal getEfectivoEsperado() {
            return efectivoEsperado;
        }

        public void setEfectivoEsperado(BigDecimal efectivoEsperado) {
            this.efectivoEsperado = efectivoEsperado;
        }

        public int getCantidadVentas() {
            return cantidadVentas;
        }

        public void setCantidadVentas(int cantidadVentas) {
            this.cantidadVentas = cantidadVentas;
        }
    }

    public ResumenTurnoCajaDto obtenerResumenTurno(CierreCaja caja) {
        ResumenTurnoCajaDto res = new ResumenTurnoCajaDto();
        if (caja == null)
            return res;

        LocalDateTime inicio = caja.getFechaApertura();
        LocalDateTime fin = caja.getFechaCierre() != null ? caja.getFechaCierre() : LocalDateTime.now();

        res.setMontoApertura(caja.getMontoApertura() != null ? caja.getMontoApertura() : BigDecimal.ZERO);

        List<Venta> ventasTurno = ventaRepo.findByFechaHoraBetween(inicio, fin).stream()
                .filter(v -> Boolean.TRUE.equals(v.getEstado()))
                .toList();

        res.setCantidadVentas(ventasTurno.size());

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal digital = BigDecimal.ZERO;

        for (Venta v : ventasTurno) {
            BigDecimal totVenta = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            totalVentas = totalVentas.add(totVenta);

            Pago p = v.getPago();
            if (p != null && p.getPagoMedio() != null && !p.getPagoMedio().isEmpty()) {
                for (PagoMedio pm : p.getPagoMedio()) {
                    BigDecimal m = pm.getMonto() != null ? pm.getMonto() : BigDecimal.ZERO;
                    String nom = (pm.getMetodoPago() != null && pm.getMetodoPago().getNombre() != null)
                            ? pm.getMetodoPago().getNombre().toUpperCase()
                            : "";
                    if (nom.contains("EFECTIVO")) {
                        efectivo = efectivo.add(m);
                    } else {
                        digital = digital.add(m);
                    }
                }
            } else {
                // Fallback por defecto si no tiene desglose registrado
                efectivo = efectivo.add(totVenta);
            }
        }

        res.setTotalVentas(totalVentas);
        res.setVentasEfectivo(efectivo);
        res.setVentasDigitales(digital);

        LocalDate desde = inicio.toLocalDate();
        LocalDate hasta = fin.toLocalDate();
        BigDecimal gastos = gastoRepo.findByFechaBetweenOrderByFechaDesc(desde, hasta).stream()
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        res.setTotalGastos(gastos);
        res.setEfectivoEsperado(res.getMontoApertura().add(efectivo).subtract(gastos));

        return res;
    }

    public ResumenTurnoCajaDto obtenerResumenTurno() {
        return obtenerCajaAbierta().map(this::obtenerResumenTurno).orElseGet(ResumenTurnoCajaDto::new);
    }

    @Transactional
    public CierreCaja cerrarCaja(BigDecimal efectivoReal) {
        CierreCaja caja = obtenerCajaAbierta()
                .orElseThrow(() -> new IllegalStateException("No hay una caja abierta para cerrar en este momento."));
        return cerrarCaja(caja.getIdCaja(), efectivoReal);
    }

    @Transactional
    public CierreCaja cerrarCaja(Integer idCaja, BigDecimal efectivoReal) {
        CierreCaja caja = cierreRepo.findById(idCaja)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada: " + idCaja));

        LocalDateTime fin = LocalDateTime.now();
        caja.setFechaCierre(fin);

        ResumenTurnoCajaDto resumen = obtenerResumenTurno(caja);

        caja.setTotalVenta(resumen.getTotalVentas());
        caja.setTotalGasto(resumen.getTotalGastos());

        // El arqueo físico se contrasta contra el efectivo real esperado (Apertura +
        // Ventas Efectivo - Gastos)
        BigDecimal esperadoEfectivo = resumen.getEfectivoEsperado();
        if (efectivoReal != null) {
            caja.setDiferencia(efectivoReal.subtract(esperadoEfectivo));
        } else {
            caja.setDiferencia(BigDecimal.ZERO);
        }

        return cierreRepo.save(caja);
    }

    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorId(Integer idVenta) {
        return ventaRepo.findById(idVenta);
    }

    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorPrecuenta(Integer idPrecuenta) {
        return ventaRepo.findByPrecuenta_IdPrecuenta(idPrecuenta);
    }

    @Transactional(readOnly = true)
    public Optional<Venta> obtenerUltimaVentaPorPedido(Integer idPedido) {
        List<Venta> ventas = ventaRepo.findByPedido_IdPedidoOrderByFechaHoraDesc(idPedido);
        return ventas.isEmpty() ? Optional.empty() : Optional.of(ventas.get(0));
    }

    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorPedido(Integer idPedido) {
        return obtenerUltimaVentaPorPedido(idPedido);
    }
}
