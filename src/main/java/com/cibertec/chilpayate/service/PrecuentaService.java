package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrecuentaService {

    private final PrecuentaRepository precuentaRepo;
    private final PrecuentaDetalleRepository precuentaDetalleRepo;
    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detallePedidoRepo;
    private final MesaRepository mesaRepo;
    private final DespachoService despachoService;

    public PrecuentaService(PrecuentaRepository precuentaRepo,
                            PrecuentaDetalleRepository precuentaDetalleRepo,
                            PedidoRepository pedidoRepo,
                            DetallePedidoRepository detallePedidoRepo,
                            MesaRepository mesaRepo,
                            DespachoService despachoService) {
        this.precuentaRepo = precuentaRepo;
        this.precuentaDetalleRepo = precuentaDetalleRepo;
        this.pedidoRepo = pedidoRepo;
        this.detallePedidoRepo = detallePedidoRepo;
        this.mesaRepo = mesaRepo;
        this.despachoService = despachoService;
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Genera una precuenta total para todos los ítems aún no asignados a una precuenta activa.
     */
    @Transactional
    public Precuenta generarPrecuentaCompleta(Integer idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));

        if (!despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            throw new IllegalStateException("No se puede generar la precuenta: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
        }

        // Si ya existe alguna precuenta activa para este pedido, retornarla para evitar duplicados
        List<Precuenta> activas = precuentaRepo.findByPedido_IdPedidoAndEstadoNot(idPedido, Precuenta.EstadoPrecuenta.ANULADA);
        if (!activas.isEmpty()) {
            return activas.get(0);
        }

        List<DetallePedido> disponibles = obtenerItemsDisponiblesParaDividir(idPedido);
        if (disponibles.isEmpty()) {
            throw new IllegalStateException("Todos los productos del pedido ya se encuentran en una precuenta o han sido pagados.");
        }

        List<Integer> ids = disponibles.stream().map(DetallePedido::getIdDetallePedido).collect(Collectors.toList());
        return generarPrecuentaDividida(idPedido, ids);
    }

    @Transactional
    public Precuenta generarPrecuenta(Integer idPedido) {
        return generarPrecuentaCompleta(idPedido);
    }

    @Transactional
    public void eliminarPrecuenta(Integer idPedido) {
        reiniciarPrecuentasPorPedido(idPedido);
    }

    /**
     * Genera una precuenta dividida por productos específicos seleccionados.
     */
    @Transactional
    public Precuenta generarPrecuentaDividida(Integer idPedido, List<Integer> idDetalles) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));

        // Regla de Negocio: Mientras no termine de despachar todos los pedidos (o dar clic en Servido/Entregado a Mesa),
        // no se puede agregar ni generar precuenta
        if (!despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            throw new IllegalStateException("No se puede generar la precuenta: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
        }

        if (idDetalles == null || idDetalles.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un producto para la precuenta.");
        }

        // Obtener ítems ya asignados a precuentas activas (PENDIENTE o PAGADA)
        Set<Integer> itemsYaAsignados = obtenerIdsItemsAsignados(idPedido);

        List<DetallePedido> detallesParaPrecuenta = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Integer idDet : idDetalles) {
            if (itemsYaAsignados.contains(idDet)) {
                throw new IllegalStateException("El ítem #" + idDet + " ya fue asignado a otra precuenta activa.");
            }
            DetallePedido det = detallePedidoRepo.findById(idDet)
                    .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado: " + idDet));

            if (!det.getPedido().getIdPedido().equals(idPedido)) {
                throw new IllegalArgumentException("El ítem #" + idDet + " no pertenece a este pedido.");
            }
            if (det.getEstado() == DetallePedido.EstadoDetalle.ANULADO) {
                continue;
            }

            detallesParaPrecuenta.add(det);
            if (det.getPrecioActual() != null) {
                total = total.add(det.getPrecioActual());
            }
        }

        if (detallesParaPrecuenta.isEmpty()) {
            throw new IllegalArgumentException("No hay ítems válidos seleccionados.");
        }

        // Si es delivery y es la primera precuenta, o si se incluye costo de delivery
        List<Precuenta> activasPrevias = precuentaRepo.findByPedido_IdPedidoAndEstadoNot(idPedido, Precuenta.EstadoPrecuenta.ANULADA);
        int siguienteNumero = activasPrevias.size() + 1;

        if (pedido.getTipo() == Pedido.TipoPedido.DELIVERY && activasPrevias.isEmpty()) {
            if (pedido.getPedidoDelivery() != null && pedido.getPedidoDelivery().getCostoDelivery() != null) {
                total = total.add(pedido.getPedidoDelivery().getCostoDelivery());
            }
        }

        BigDecimal divisor = new BigDecimal("1.18");
        BigDecimal subtotal = total.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);

        Precuenta precuenta = new Precuenta();
        precuenta.setPedido(pedido);
        precuenta.setNumeroPrecuenta(siguienteNumero);
        precuenta.setCodigo("PREC-" + pedido.getCodigo() + "-" + siguienteNumero);
        precuenta.setSubtotal(subtotal);
        precuenta.setIgv(igv);
        precuenta.setTotal(total);
        precuenta.setFechaHora(LocalDateTime.now());
        precuenta.setEstado(Precuenta.EstadoPrecuenta.PENDIENTE);

        Precuenta guardada = precuentaRepo.save(precuenta);

        for (DetallePedido det : detallesParaPrecuenta) {
            PrecuentaDetalle pd = new PrecuentaDetalle(guardada, det, det.getPrecioActual() != null ? det.getPrecioActual() : BigDecimal.ZERO);
            precuentaDetalleRepo.save(pd);
            guardada.getDetalles().add(pd);
        }

        if (pedido.getEstado() != Pedido.EstadoPedido.PRECUENTA) {
            pedido.setEstado(Pedido.EstadoPedido.PRECUENTA);
            pedidoRepo.save(pedido);
        }

        // Regla de Negocio: Al generar la precuenta, bloquear el acceso a la mesa desde Salón con estado BLOQUEADA
        if (pedido.getMesa() != null) {
            Mesa mesa = pedido.getMesa();
            mesa.setEstado(Mesa.EstadoMesa.BLOQUEADA);
            mesaRepo.save(mesa);
        }

        return guardada;
    }

    @Transactional
    public Precuenta generarPrecuentaDividida(Integer idPedido, List<Integer> idDetalles, String nombreCliente) {
        return generarPrecuentaDividida(idPedido, idDetalles);
    }

    @Transactional
    public Precuenta generarPrecuentaDividida(Integer idPedido, List<Integer> idDetalles, String nombreCliente, String dniRuc, String tipoComprobante) {
        return generarPrecuentaDividida(idPedido, idDetalles);
    }

    /**
     * Genera un split completo en N precuentas a partir del mapa (idDetallePedido -> numeroCuenta).
     * Si existían precuentas previas pendientes, las reinicia para asegurar integridad.
     */
    @Transactional
    public List<Precuenta> generarSplitCompleto(Integer idPedido, Map<Integer, Integer> asignaciones) {
        if (!despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            throw new IllegalStateException("No se puede generar la división de precuentas: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
        }

        if (asignaciones == null || asignaciones.isEmpty()) {
            throw new IllegalArgumentException("Debe asignar los platos a las subcuentas correspondientes.");
        }

        // 1. Reiniciar precuentas pendientes previas
        reiniciarPrecuentasPorPedido(idPedido);

        // 2. Agrupar ítems por número de subcuenta asignada (1, 2, 3...)
        Map<Integer, List<Integer>> cuentas = new TreeMap<>();
        asignaciones.forEach((idDet, numCuenta) -> {
            if (idDet != null && numCuenta != null && numCuenta > 0) {
                cuentas.computeIfAbsent(numCuenta, k -> new ArrayList<>()).add(idDet);
            }
        });

        if (cuentas.isEmpty()) {
            throw new IllegalArgumentException("No se seleccionó ningún plato para las subcuentas.");
        }

        // 3. Crear cada subcuenta en orden
        List<Precuenta> resultado = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : cuentas.entrySet()) {
            List<Integer> ids = entry.getValue();
            if (!ids.isEmpty()) {
                Precuenta p = generarPrecuentaDividida(idPedido, ids);
                resultado.add(p);
            }
        }

        return resultado;
    }

    /**
     * Retorna los ítems del pedido que aún NO están en una precuenta PENDIENTE o PAGADA.
     */
    public List<DetallePedido> obtenerItemsDisponiblesParaDividir(Integer idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido).orElse(null);
        if (pedido == null || pedido.getDetallePedido() == null) return Collections.emptyList();

        Set<Integer> asignados = obtenerIdsItemsAsignados(idPedido);

        return pedido.getDetallePedido().stream()
                .filter(d -> d.getEstado() != DetallePedido.EstadoDetalle.ANULADO)
                .filter(d -> !asignados.contains(d.getIdDetallePedido()))
                .collect(Collectors.toList());
    }

    private Set<Integer> obtenerIdsItemsAsignados(Integer idPedido) {
        List<Precuenta> activas = precuentaRepo.findByPedido_IdPedidoAndEstadoNot(idPedido, Precuenta.EstadoPrecuenta.ANULADA);
        Set<Integer> ids = new HashSet<>();
        for (Precuenta prec : activas) {
            List<PrecuentaDetalle> detalles = precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(prec.getIdPrecuenta());
            for (PrecuentaDetalle pd : detalles) {
                if (pd.getDetallePedido() != null) {
                    ids.add(pd.getDetallePedido().getIdDetallePedido());
                }
            }
        }
        return ids;
    }

    public List<Precuenta> listarPrecuentasPorPedido(Integer idPedido) {
        return precuentaRepo.findByPedido_IdPedidoAndEstadoNotOrderByNumeroPrecuentaAsc(idPedido, Precuenta.EstadoPrecuenta.ANULADA);
    }

    public List<Precuenta> listarPrecuentasPendientes(Integer idPedido) {
        return precuentaRepo.findByPedido_IdPedidoAndEstado(idPedido, Precuenta.EstadoPrecuenta.PENDIENTE);
    }

    public Optional<Precuenta> obtenerPorId(Integer idPrecuenta) {
        return precuentaRepo.findById(idPrecuenta);
    }

    /**
     * Anula una precuenta y elimina sus detalles para liberar los productos (Requisito 2).
     */
    @Transactional
    public void anularPrecuenta(Integer idPrecuenta) {
        Precuenta precuenta = precuentaRepo.findById(idPrecuenta)
                .orElseThrow(() -> new IllegalArgumentException("Precuenta no encontrada: " + idPrecuenta));

        if (precuenta.getEstado() == Precuenta.EstadoPrecuenta.PAGADA) {
            throw new IllegalStateException("No se puede anular una precuenta que ya fue cobrada.");
        }

        // Requisito 2: Eliminar los detalles de esa precuenta para liberar los productos
        precuentaDetalleRepo.deleteByPrecuenta_IdPrecuenta(idPrecuenta);
        if (precuenta.getDetalles() != null) {
            precuenta.getDetalles().clear();
        }

        precuenta.setEstado(Precuenta.EstadoPrecuenta.ANULADA);
        precuentaRepo.save(precuenta);

        verificarYRestaurarEstadoMesa(precuenta.getPedido());
    }

    /**
     * Elimina físicamente una precuenta no cobrada y libera sus productos (Requisito 7 y 8).
     */
    @Transactional
    public void eliminarPrecuentaPorId(Integer idPrecuenta) {
        Precuenta precuenta = precuentaRepo.findById(idPrecuenta)
                .orElseThrow(() -> new IllegalArgumentException("Precuenta no encontrada: " + idPrecuenta));

        if (precuenta.getEstado() == Precuenta.EstadoPrecuenta.PAGADA) {
            throw new IllegalStateException("No se puede eliminar una precuenta que ya fue cobrada.");
        }

        Pedido pedido = precuenta.getPedido();

        // 1. Eliminar los detalles de la precuenta para liberar productos
        precuentaDetalleRepo.deleteByPrecuenta_IdPrecuenta(idPrecuenta);
        if (precuenta.getDetalles() != null) {
            precuenta.getDetalles().clear();
        }

        // 2. Eliminar la entidad Precuenta de la BD
        precuentaRepo.delete(precuenta);

        // 3. Restaurar estado de pedido y mesa si no quedan precuentas activas
        verificarYRestaurarEstadoMesa(pedido);
    }

    public boolean tienePrecuentasPagadas(Integer idPedido) {
        if (idPedido == null) return false;
        return precuentaRepo.existsByPedido_IdPedidoAndEstado(idPedido, Precuenta.EstadoPrecuenta.PAGADA);
    }

    /**
     * Elimina todas las precuentas pendientes de un pedido y restaura la mesa para permitir crear precuenta única (Requisito 8 y 9).
     * Si alguna subcuenta ya fue pagada, bloquea la operación para proteger la consistencia contable.
     */
    @Transactional
    public void reiniciarPrecuentasPorPedido(Integer idPedido) {
        if (tienePrecuentasPagadas(idPedido)) {
            throw new IllegalStateException("No se pueden revertir las precuentas porque al menos una subcuenta ya ha sido pagada.");
        }
        List<Precuenta> pendientes = precuentaRepo.findByPedido_IdPedidoAndEstado(idPedido, Precuenta.EstadoPrecuenta.PENDIENTE);
        for (Precuenta p : pendientes) {
            precuentaDetalleRepo.deleteByPrecuenta_IdPrecuenta(p.getIdPrecuenta());
            precuentaRepo.delete(p);
        }
        Pedido pedido = pedidoRepo.findById(idPedido).orElse(null);
        if (pedido != null) {
            verificarYRestaurarEstadoMesa(pedido);
        }
    }

    /**
     * Genera una nueva precuenta con los productos que aún no han sido asignados a ninguna precuenta.
     */
    @Transactional
    public Precuenta generarPrecuentaParaItemsRestantes(Integer idPedido) {
        if (!despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            throw new IllegalStateException("No se puede generar la precuenta restante: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
        }
        List<DetallePedido> disponibles = obtenerItemsDisponiblesParaDividir(idPedido);
        if (disponibles.isEmpty()) {
            throw new IllegalArgumentException("No hay platos pendientes por asignar.");
        }
        List<Integer> ids = disponibles.stream().map(DetallePedido::getIdDetallePedido).collect(Collectors.toList());
        return generarPrecuentaDividida(idPedido, ids);
    }

    private void verificarYRestaurarEstadoMesa(Pedido pedido) {
        if (pedido == null) return;
        List<Precuenta> activas = precuentaRepo.findByPedido_IdPedidoAndEstadoNot(pedido.getIdPedido(), Precuenta.EstadoPrecuenta.ANULADA);
        if (activas.isEmpty()) {
            pedido.setEstado(Pedido.EstadoPedido.ACTIVA);
            pedidoRepo.save(pedido);
            if (pedido.getMesa() != null) {
                Mesa mesa = pedido.getMesa();
                mesa.setEstado(Mesa.EstadoMesa.OCUPADA);
                mesaRepo.save(mesa);
            }
        }
    }

    public PrecuentaDto construirDtoDePrecuenta(Precuenta precuenta) {
        if (precuenta == null) return null;
        Pedido pedido = precuenta.getPedido();

        PrecuentaDto dto = new PrecuentaDto();
        dto.setIdPrecuenta(precuenta.getIdPrecuenta());
        dto.setIdPedido(pedido.getIdPedido());
        dto.setCodigo(precuenta.getCodigo());
        dto.setNumeroPrecuenta(precuenta.getNumeroPrecuenta());
        dto.setNombreCliente("Precuenta #" + precuenta.getNumeroPrecuenta());
        dto.setDniRuc("");
        dto.setTipoComprobante("BOLETA");
        dto.setFechaEmision(precuenta.getFechaHora() != null ? precuenta.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        dto.setSubtotal(precuenta.getSubtotal() != null ? precuenta.getSubtotal() : BigDecimal.ZERO);
        dto.setIgv(precuenta.getIgv() != null ? precuenta.getIgv() : BigDecimal.ZERO);
        dto.setTotal(precuenta.getTotal() != null ? precuenta.getTotal() : BigDecimal.ZERO);
        dto.setEstado(precuenta.getEstado() != null ? precuenta.getEstado().name() : "PENDIENTE");

        if (pedido.getMesa() != null) {
            dto.setIdMesa(pedido.getMesa().getIdMesa());
            dto.setNumeroMesa(pedido.getMesa().getNumero());
            dto.setSalon(pedido.getMesa().getSalon());
        } else {
            dto.setNumeroMesa(0);
            dto.setSalon("DELIVERY");
        }

        if (pedido.getUsuario() != null) {
            String mozo = pedido.getUsuario().getNombres() != null ? pedido.getUsuario().getNombres() : pedido.getUsuario().getUsername();
            if (pedido.getUsuario().getApellidos() != null) {
                mozo += " " + pedido.getUsuario().getApellidos();
            }
            dto.setMozo(mozo);
        } else {
            dto.setMozo("Mozo Asignado");
        }

        dto.setCliente(pedido.getCliente() != null && pedido.getCliente().getNombre() != null ? pedido.getCliente().getNombre() : "Cliente Casual");

        List<PrecuentaItemDto> items = new ArrayList<>();
        List<PrecuentaDetalle> detalles = precuentaDetalleRepo.findByPrecuenta_IdPrecuenta(precuenta.getIdPrecuenta());

        for (PrecuentaDetalle pd : detalles) {
            DetallePedido d = pd.getDetallePedido();
            PrecuentaItemDto item = new PrecuentaItemDto();
            item.setIdDetalle(d.getIdDetallePedido());
            item.setProducto(d.getProducto() != null ? d.getProducto().getNombre() : "Plato");
            item.setCantidad(1);
            item.setPrecioUnitario(pd.getPrecio());
            item.setSubtotal(pd.getPrecio());
            item.setNota(d.getNota());
            item.setEstado(d.getEstado() != null ? d.getEstado().name() : "BORRADOR");

            if (d.getDetallePedidoCrema() != null && !d.getDetallePedidoCrema().isEmpty()) {
                List<String> cremas = d.getDetallePedidoCrema().stream()
                        .filter(dc -> dc.getCrema() != null)
                        .map(dc -> dc.getCrema().getNombre())
                        .collect(Collectors.toList());
                item.setCremas(String.join(", ", cremas));
            }

            if (d.getDetallePedidoSalsa() != null && !d.getDetallePedidoSalsa().isEmpty()) {
                List<String> salsas = d.getDetallePedidoSalsa().stream()
                        .filter(ds -> ds.getSalsa() != null)
                        .map(ds -> ds.getSalsa().getNombre())
                        .collect(Collectors.toList());
                item.setSalsas(String.join(", ", salsas));
            }

            items.add(item);
        }

        dto.setItems(items);
        return dto;
    }


    public Optional<String> obtenerPrecuentaJsonPorId(Integer idPrecuenta) {
        return precuentaRepo.findById(idPrecuenta).map(p -> {
            try {
                return objectMapper.writeValueAsString(construirDtoDePrecuenta(p));
            } catch (Exception e) {
                return "{}";
            }
        });
    }

    public Optional<String> obtenerPrecuentaJson(Integer idPedido) {
        // Retorna la primera precuenta pendiente, o la última generada
        List<Precuenta> precuentas = precuentaRepo.findByPedido_IdPedidoOrderByNumeroPrecuentaAsc(idPedido);
        if (!precuentas.isEmpty()) {
            // Preferir pendiente
            Precuenta target = precuentas.stream()
                    .filter(p -> p.getEstado() == Precuenta.EstadoPrecuenta.PENDIENTE)
                    .findFirst()
                    .orElse(precuentas.get(precuentas.size() - 1));
            return obtenerPrecuentaJsonPorId(target.getIdPrecuenta());
        }

        // Si aún no se creó en BD, NO se debe crear automáticamente
        return Optional.empty();
    }

    public boolean tienePrecuenta(Integer idPedido) {
        if (idPedido == null) return false;
        List<Precuenta> list = precuentaRepo.findByPedido_IdPedidoAndEstadoNot(idPedido, Precuenta.EstadoPrecuenta.ANULADA);
        return !list.isEmpty();
    }

    public List<PrecuentaDto> listarPrecuentaDtosPorPedido(Integer idPedido) {
        if (idPedido == null) return Collections.emptyList();
        List<Precuenta> list = precuentaRepo.findByPedido_IdPedidoOrderByNumeroPrecuentaAsc(idPedido);
        return list.stream()
                .filter(p -> p.getEstado() != Precuenta.EstadoPrecuenta.ANULADA)
                .map(this::construirDtoDePrecuenta)
                .collect(Collectors.toList());
    }

    public static class PrecuentaDto {
        private Integer idPrecuenta;
        private Integer idPedido;
        private String codigo;
        private Integer numeroPrecuenta;
        private String nombreCliente;
        private String dniRuc;
        private String tipoComprobante;
        private Integer idMesa;
        private Integer numeroMesa;
        private String salon;
        private String mozo;
        private String cliente;
        private String fechaEmision;
        private List<PrecuentaItemDto> items;
        private BigDecimal subtotal;
        private BigDecimal igv;
        private BigDecimal costoDelivery;
        private BigDecimal total;
        private String estado;

        public Integer getIdPrecuenta() { return idPrecuenta; }
        public void setIdPrecuenta(Integer idPrecuenta) { this.idPrecuenta = idPrecuenta; }
        public Integer getIdPedido() { return idPedido; }
        public void setIdPedido(Integer idPedido) { this.idPedido = idPedido; }
        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public Integer getNumeroPrecuenta() { return numeroPrecuenta; }
        public void setNumeroPrecuenta(Integer numeroPrecuenta) { this.numeroPrecuenta = numeroPrecuenta; }
        public String getNombreCliente() { return nombreCliente; }
        public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
        public String getDniRuc() { return dniRuc; }
        public void setDniRuc(String dniRuc) { this.dniRuc = dniRuc; }
        public String getTipoComprobante() { return tipoComprobante; }
        public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
        public Integer getIdMesa() { return idMesa; }
        public void setIdMesa(Integer idMesa) { this.idMesa = idMesa; }
        public Integer getNumeroMesa() { return numeroMesa; }
        public void setNumeroMesa(Integer numeroMesa) { this.numeroMesa = numeroMesa; }
        public String getSalon() { return salon; }
        public void setSalon(String salon) { this.salon = salon; }
        public String getMozo() { return mozo; }
        public void setMozo(String mozo) { this.mozo = mozo; }
        public String getCliente() { return cliente; }
        public void setCliente(String cliente) { this.cliente = cliente; }
        public String getFechaEmision() { return fechaEmision; }
        public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }
        public List<PrecuentaItemDto> getItems() { return items; }
        public void setItems(List<PrecuentaItemDto> items) { this.items = items; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public BigDecimal getIgv() { return igv; }
        public void setIgv(BigDecimal igv) { this.igv = igv; }
        public BigDecimal getCostoDelivery() { return costoDelivery; }
        public void setCostoDelivery(BigDecimal costoDelivery) { this.costoDelivery = costoDelivery; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }

    public static class PrecuentaItemDto {
        private Integer idDetalle;
        private String producto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        private String nota;
        private String cremas;
        private String salsas;
        private String estado;

        public Integer getIdDetalle() { return idDetalle; }
        public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }
        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public String getNota() { return nota; }
        public void setNota(String nota) { this.nota = nota; }
        public String getCremas() { return cremas; }
        public void setCremas(String cremas) { this.cremas = cremas; }
        public String getSalsas() { return salsas; }
        public void setSalsas(String salsas) { this.salsas = salsas; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }
}
