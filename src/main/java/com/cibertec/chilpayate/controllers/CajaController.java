package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/caja")
public class CajaController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CajaController.class);

    private final CajaService cajaService;
    private final PedidoService pedidoService;
    private final GastoService gastoService;
    private final MesaService mesaService;
    private final PrecuentaService precuentaService;
    private final DespachoService despachoService;

    public CajaController(CajaService cajaService,
                          PedidoService pedidoService,
                          GastoService gastoService,
                          MesaService mesaService,
                          PrecuentaService precuentaService,
                          DespachoService despachoService) {
        this.cajaService = cajaService;
        this.pedidoService = pedidoService;
        this.gastoService = gastoService;
        this.mesaService = mesaService;
        this.precuentaService = precuentaService;
        this.despachoService = despachoService;
    }

    @GetMapping
    public String list(Model model) {
        List<Pedido> pendientes = pedidoService.listarActivos().stream()
                .filter(p -> p.getEstado() != Pedido.EstadoPedido.PAGADA)
                .filter(p -> p.getPedidoDelivery() == null || !Boolean.TRUE.equals(p.getPedidoDelivery().getPagado()))
                .toList();
        model.addAttribute("pedidosPendientes", pendientes);
        model.addAttribute("ventasHoy", cajaService.listarVentasHoy());
        model.addAttribute("cajaAbierta", cajaService.obtenerCajaAbierta().orElse(null));
        return "caja/list";
    }

    @GetMapping("/terminal")
    public String terminal(@RequestParam(required = false) Integer verPrecuenta,
                           @RequestParam(required = false) Integer verPrecuentaId,
                           Model model) {
        List<Mesa> mesas = mesaService.listarActivas();
        Map<Integer, Pedido> pedidosPorMesa = new HashMap<>();
        Map<Integer, List<Precuenta>> precuentasPorPedido = new HashMap<>();
        Map<Integer, String> precuentasJson = new HashMap<>();
        Map<Integer, Boolean> despachoCompletadoPorPedido = new HashMap<>();
        Map<Integer, String> motivoBloqueoPorPedido = new HashMap<>();

        for (Mesa m : mesas) {
            if (m.getEstado() == Mesa.EstadoMesa.OCUPADA || m.getEstado() == Mesa.EstadoMesa.BLOQUEADA) {
                pedidoService.obtenerPorMesaActiva(m.getIdMesa())
                        .ifPresent(p -> {
                            pedidosPorMesa.put(m.getIdMesa(), p);
                            List<Precuenta> precs = precuentaService.listarPrecuentasPorPedido(p.getIdPedido());
                            precuentasPorPedido.put(p.getIdPedido(), precs);
                            if (!precs.isEmpty()) {
                                precuentaService.obtenerPrecuentaJson(p.getIdPedido())
                                        .ifPresent(json -> precuentasJson.put(p.getIdPedido(), json));
                            }

                            boolean despachado = despachoService.estaTotalmenteDespachado(p.getIdPedido());
                            despachoCompletadoPorPedido.put(p.getIdPedido(), despachado);
                            if (!despachado) {
                                motivoBloqueoPorPedido.put(p.getIdPedido(), despachoService.obtenerMotivoBloqueoDespacho(p.getIdPedido()));
                            }
                        });
            }
        }

        List<Pedido> pedidosDelivery = pedidoService.listarActivos().stream()
                .filter(p -> p.getTipo() == Pedido.TipoPedido.DELIVERY)
                .toList();

        for (Pedido d : pedidosDelivery) {
            List<Precuenta> precs = precuentaService.listarPrecuentasPorPedido(d.getIdPedido());
            precuentasPorPedido.put(d.getIdPedido(), precs);
            if (!precs.isEmpty()) {
                precuentaService.obtenerPrecuentaJson(d.getIdPedido())
                        .ifPresent(json -> precuentasJson.put(d.getIdPedido(), json));
            }

            boolean despachado = despachoService.estaTotalmenteDespachado(d.getIdPedido());
            despachoCompletadoPorPedido.put(d.getIdPedido(), despachado);
            if (!despachado) {
                motivoBloqueoPorPedido.put(d.getIdPedido(), despachoService.obtenerMotivoBloqueoDespacho(d.getIdPedido()));
            }
        }

        model.addAttribute("mesas", mesas);
        model.addAttribute("pedidosPorMesa", pedidosPorMesa);
        model.addAttribute("precuentasPorPedido", precuentasPorPedido);
        model.addAttribute("precuentasJson", precuentasJson);
        model.addAttribute("despachoCompletadoPorPedido", despachoCompletadoPorPedido);
        model.addAttribute("motivoBloqueoPorPedido", motivoBloqueoPorPedido);
        model.addAttribute("verPrecuentaId", verPrecuentaId != null ? verPrecuentaId : verPrecuenta);
        model.addAttribute("cajaAbierta", cajaService.obtenerCajaAbierta().orElse(null));
        model.addAttribute("pedidosDelivery", pedidosDelivery);
        return "caja/terminal";
    }


    @GetMapping("/precuenta/pedido/{idPedido}")
    public String verVistaPrecuenta(@PathVariable Integer idPedido,
                                    @RequestParam(value = "split", required = false, defaultValue = "false") boolean modoSplit,
                                    Model model,
                                    RedirectAttributes ra) {
        Pedido pedido = pedidoService.obtenerPorId(idPedido).orElse(null);
        if (pedido == null) {
            ra.addFlashAttribute("error", "Pedido no encontrado.");
            return "redirect:/caja/terminal";
        }

        List<Precuenta> precuentas = precuentaService.listarPrecuentasPorPedido(idPedido);
        List<DetallePedido> detallesActivos = (pedido.getDetallePedido() != null)
                ? pedido.getDetallePedido().stream()
                    .filter(d -> d.getEstado() != DetallePedido.EstadoDetalle.ANULADO)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        boolean despachado = despachoService.estaTotalmenteDespachado(idPedido);
        String motivoBloqueo = !despachado ? despachoService.obtenerMotivoBloqueoDespacho(idPedido) : null;

        if (!despachado && precuentas.isEmpty()) {
            ra.addFlashAttribute("error", "No se puede generar precuenta: " +
                    (motivoBloqueo != null && !motivoBloqueo.isBlank() ? motivoBloqueo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
            return "redirect:/caja/terminal";
        }

        int maxCuentas = Math.max(1, detallesActivos.size());

        boolean tienePagadas = precuentaService.tienePrecuentasPagadas(idPedido);
        List<DetallePedido> itemsRestantes = precuentaService.obtenerItemsDisponiblesParaDividir(idPedido);

        model.addAttribute("pedido", pedido);
        model.addAttribute("mesa", pedido.getMesa());
        model.addAttribute("precuentas", precuentas);
        model.addAttribute("tienePrecuentas", !precuentas.isEmpty());
        model.addAttribute("tienePagadas", tienePagadas);
        model.addAttribute("puedeRevertir", !tienePagadas && !precuentas.isEmpty());
        model.addAttribute("itemsRestantes", itemsRestantes);
        model.addAttribute("tieneItemsRestantes", !itemsRestantes.isEmpty());
        model.addAttribute("detalles", detallesActivos);
        model.addAttribute("maxCuentas", maxCuentas);
        model.addAttribute("modoSplit", modoSplit);
        model.addAttribute("despachado", despachado);
        model.addAttribute("motivoBloqueo", motivoBloqueo);

        return "caja/precuenta";
    }

    @PostMapping("/precuenta/generar-unica/{idPedido}")
    public String generarPrecuentaUnicaDesdeVista(@PathVariable Integer idPedido, RedirectAttributes ra) {
        if (!despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            ra.addFlashAttribute("error", "No se puede generar la precuenta: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
            return "redirect:/caja/terminal";
        }
        try {
            Precuenta p = precuentaService.generarPrecuentaCompleta(idPedido);
            ra.addFlashAttribute("success", "Precuenta única generada exitosamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al generar precuenta: " + e.getMessage());
        }
        return "redirect:/caja/precuenta/pedido/" + idPedido;
    }

 
    @PostMapping("/precuenta/guardar-split/{idPedido}")
    public String guardarSplitDesdeVista(@PathVariable Integer idPedido,
                                         @RequestParam Map<String, String> params,
                                         RedirectAttributes ra) {
        if (!despachoService.estaTotalmenteDespachado(idPedido)) {
            String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
            ra.addFlashAttribute("error", "No se puede generar la división de precuentas: " +
                    (motivo != null && !motivo.isBlank() ? motivo + ". " : "") +
                    "El mozo debe confirmar 'Servido / Entregado a Mesa' antes de emitir precuentas.");
            return "redirect:/caja/terminal";
        }
        try {
            Map<Integer, Integer> asignaciones = new HashMap<>();
            params.forEach((key, val) -> {
                if (key.startsWith("cuenta_")) {
                    try {
                        Integer idDet = Integer.parseInt(key.substring(7));
                        Integer numCuenta = Integer.parseInt(val);
                        asignaciones.put(idDet, numCuenta);
                    } catch (NumberFormatException ignored) {}
                }
            });

            if (asignaciones.isEmpty()) {
                throw new IllegalArgumentException("Debe asignar los platos a las subcuentas.");
            }

            List<Precuenta> precs = precuentaService.generarSplitCompleto(idPedido, asignaciones);
            ra.addFlashAttribute("success", "Se generaron " + precs.size() + " subcuentas correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar división de precuentas: " + e.getMessage());
            return "redirect:/caja/precuenta/pedido/" + idPedido + "?split=true";
        }
        return "redirect:/caja/precuenta/pedido/" + idPedido;
    }

    @PostMapping("/precuenta/revertir-todo/{idPedido}")
    public String revertirTodoDesdeVista(@PathVariable Integer idPedido, RedirectAttributes ra) {
        try {
            if (precuentaService.tienePrecuentasPagadas(idPedido)) {
                ra.addFlashAttribute("error", "No se pueden revertir las precuentas porque al menos una subcuenta ya ha sido pagada.");
                return "redirect:/caja/precuenta/pedido/" + idPedido;
            }
            precuentaService.reiniciarPrecuentasPorPedido(idPedido);
            ra.addFlashAttribute("success", "Precuentas revertidas. Todos los productos están disponibles nuevamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo revertir: " + e.getMessage());
        }
        return "redirect:/caja/precuenta/pedido/" + idPedido;
    }

    @PostMapping("/precuenta/generar-restantes/{idPedido}")
    public String generarPrecuentaRestantesDesdeVista(@PathVariable Integer idPedido, RedirectAttributes ra) {
        try {
            Precuenta p = precuentaService.generarPrecuentaParaItemsRestantes(idPedido);
            ra.addFlashAttribute("success", "Se generó la Precuenta #" + p.getNumeroPrecuenta() + " para los platos restantes.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al generar precuenta restante: " + e.getMessage());
        }
        return "redirect:/caja/precuenta/pedido/" + idPedido;
    }

 
    @GetMapping("/precuenta/ticket/{idPrecuenta}")
    public String verTicketPrecuenta(@PathVariable Integer idPrecuenta, Model model, RedirectAttributes ra) {
        Precuenta p = precuentaService.obtenerPorId(idPrecuenta).orElse(null);
        if (p == null) {
            ra.addFlashAttribute("error", "Precuenta no encontrada.");
            return "redirect:/caja/terminal";
        }
        model.addAttribute("precuenta", p);
        model.addAttribute("dto", precuentaService.construirDtoDePrecuenta(p));
        return "caja/ticket";
    }


    /**
     * Generar Precuenta Completa (todos los productos sin dividir)
     */
    @PostMapping("/precuenta/generar/{idPedido}")
    public String generarPrecuentaCompleta(@PathVariable Integer idPedido, RedirectAttributes ra) {
        try {
            Precuenta p = precuentaService.generarPrecuentaCompleta(idPedido);
            ra.addFlashAttribute("success", "Precuenta #" + p.getNumeroPrecuenta() + " (" + p.getCodigo() + ") generada exitosamente.");
            return "redirect:/caja/terminal?verPrecuentaId=" + p.getIdPrecuenta();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al generar precuenta: " + e.getMessage());
            return "redirect:/caja/terminal";
        }
    }

    /**
     * Generar Precuenta Dividida por Productos seleccionados
     */
    /**
     * Generar Precuenta Dividida por Productos seleccionados
     */
    @PostMapping("/precuenta/dividir/{idPedido}")
    public String generarPrecuentaDividida(@PathVariable Integer idPedido,
                                           @RequestParam(required = false) List<Integer> idDetalles,
                                           RedirectAttributes ra) {
        try {
            if (idDetalles == null || idDetalles.isEmpty()) {
                throw new IllegalArgumentException("Debe seleccionar al menos un producto para la precuenta.");
            }
            Precuenta p = precuentaService.generarPrecuentaDividida(idPedido, idDetalles);
            ra.addFlashAttribute("success", "Precuenta #" + p.getNumeroPrecuenta() + " (" + p.getCodigo() + ") creada con éxito.");
            return "redirect:/caja/terminal?verPrecuentaId=" + p.getIdPrecuenta();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al dividir precuenta: " + e.getMessage());
            return "redirect:/caja/terminal";
        }
    }

    /**
     * Anular una Precuenta no cobrada (libera los productos para reasignación)
     */
    @PostMapping("/precuenta/anular/{idPrecuenta}")
    public String anularPrecuenta(@PathVariable Integer idPrecuenta, RedirectAttributes ra) {
        try {
            precuentaService.anularPrecuenta(idPrecuenta);
            ra.addFlashAttribute("success", "Precuenta anulada. Los productos han vuelto a estar disponibles para división.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo anular la precuenta: " + e.getMessage());
        }
        return "redirect:/caja/terminal";
    }

    /**
     * Eliminar físicamente una Precuenta no cobrada (Requisito 7 y 8)
     */
    @PostMapping("/precuenta/eliminar/{idPrecuenta}")
    public String eliminarPrecuenta(@PathVariable Integer idPrecuenta, RedirectAttributes ra) {
        try {
            precuentaService.eliminarPrecuentaPorId(idPrecuenta);
            ra.addFlashAttribute("success", "Precuenta eliminada y productos liberados.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar precuenta: " + e.getMessage());
        }
        return "redirect:/caja/terminal";
    }

    @PostMapping(value = "/precuenta/eliminar/{idPrecuenta}/json", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> eliminarPrecuentaJson(@PathVariable Integer idPrecuenta) {
        Map<String, Object> resp = new HashMap<>();
        try {
            precuentaService.eliminarPrecuentaPorId(idPrecuenta);
            resp.put("success", true);
            resp.put("message", "Precuenta eliminada correctamente.");
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    /**
     * Revertir Precuenta Única o reiniciar todas las precuentas de una comanda (Requisito 8 y 9)
     */
    @PostMapping("/precuenta/revertir/{idPedido}")
    public String revertirPrecuentaPedido(@PathVariable Integer idPedido, RedirectAttributes ra) {
        try {
            precuentaService.reiniciarPrecuentasPorPedido(idPedido);
            ra.addFlashAttribute("success", "Precuenta revertida exitosamente. La mesa vuelve a permitir generar Precuenta Única o Dividir.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al revertir precuenta: " + e.getMessage());
        }
        return "redirect:/caja/terminal";
    }

    @PostMapping(value = "/precuenta/reiniciar/{idPedido}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> reiniciarPrecuentasPedido(@PathVariable Integer idPedido) {
        Map<String, Object> resp = new HashMap<>();
        try {
            precuentaService.reiniciarPrecuentasPorPedido(idPedido);
            resp.put("success", true);
            resp.put("message", "Precuentas pendientes eliminadas. Mesa reiniciada para precuenta única o split.");
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    @GetMapping(value = "/precuenta/{idPedido}/json", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String obtenerPrecuentaJson(@PathVariable Integer idPedido) {
        return precuentaService.obtenerPrecuentaJson(idPedido).orElse("{}");
    }

    @GetMapping(value = "/precuenta/{idPedido}/todas/json", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<PrecuentaService.PrecuentaDto> obtenerTodasPrecuentasJson(@PathVariable Integer idPedido) {
        return precuentaService.listarPrecuentaDtosPorPedido(idPedido);
    }

    @GetMapping(value = "/precuenta/info/{idPrecuenta}/json", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String obtenerPrecuentaPorIdJson(@PathVariable Integer idPrecuenta) {
        return precuentaService.obtenerPrecuentaJsonPorId(idPrecuenta).orElse("{}");
    }

    /**
     * Retorna los productos de una comanda que aún no están asignados, junto con el resumen de saldo
     * total, asignado y subcuentas creadas para facilitar la división sin dejar saldo (Requisito 7).
     */
    @GetMapping(value = "/pedido/{idPedido}/disponibles", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> obtenerItemsDisponibles(@PathVariable Integer idPedido) {
        Map<String, Object> resp = new HashMap<>();

        Pedido pedido = pedidoService.obtenerPorId(idPedido).orElse(null);
        BigDecimal totalComanda = pedido != null ? pedidoService.calcularTotal(pedido) : BigDecimal.ZERO;

        List<Precuenta> activas = precuentaService.listarPrecuentasPorPedido(idPedido);
        BigDecimal totalAsignado = BigDecimal.ZERO;
        List<Map<String, Object>> precuentasCreadas = new ArrayList<>();
        if (activas != null) {
            for (Precuenta p : activas) {
                totalAsignado = totalAsignado.add(p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO);
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("idPrecuenta", p.getIdPrecuenta());
                pMap.put("numeroPrecuenta", p.getNumeroPrecuenta());
                pMap.put("codigo", p.getCodigo());
                pMap.put("total", p.getTotal());
                pMap.put("estado", p.getEstado() != null ? p.getEstado().name() : "PENDIENTE");
                pMap.put("cantidadItems", p.getDetalles() != null ? p.getDetalles().size() : 0);
                precuentasCreadas.add(pMap);
            }
        }

        BigDecimal saldoPendiente = totalComanda.subtract(totalAsignado);
        if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) saldoPendiente = BigDecimal.ZERO;

        List<DetallePedido> disponibles = precuentaService.obtenerItemsDisponiblesParaDividir(idPedido);
        List<Map<String, Object>> items = new ArrayList<>();
        for (DetallePedido d : disponibles) {
            Map<String, Object> map = new HashMap<>();
            map.put("idDetalle", d.getIdDetallePedido());
            map.put("producto", d.getProducto() != null ? d.getProducto().getNombre() : "Plato");
            map.put("precio", d.getPrecioActual() != null ? d.getPrecioActual() : BigDecimal.ZERO);
            map.put("nota", d.getNota() != null ? d.getNota() : "");
            items.add(map);
        }

        resp.put("items", items);
        resp.put("totalComanda", totalComanda);
        resp.put("totalAsignado", totalAsignado);
        resp.put("saldoPendiente", saldoPendiente);
        resp.put("precuentas", precuentasCreadas);
        return resp;
    }

    /**
     * Formulario de Pago de una Precuenta Específica
     */
    @GetMapping("/pago/precuenta/{idPrecuenta}")
    public String formPagoPrecuenta(@PathVariable Integer idPrecuenta, Model model, RedirectAttributes ra) {
        return precuentaService.obtenerPorId(idPrecuenta).map(prec -> {
            Integer idPedido = prec.getPedido() != null ? prec.getPedido().getIdPedido() : null;
            if (idPedido != null && !despachoService.estaTotalmenteDespachado(idPedido)) {
                String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
                ra.addFlashAttribute("error", "No se puede cobrar: " + (motivo.isBlank() ? "La comanda aún no ha sido despachada ni entregada a la mesa por el mozo." : motivo));
                return "redirect:/caja/terminal";
            }

            model.addAttribute("precuenta", prec);
            model.addAttribute("pedido", prec.getPedido());
            model.addAttribute("total", prec.getTotal());
            model.addAttribute("esPrecuentaDividida", true);
            model.addAttribute("metodosPago", cajaService.listarMetodosPago());
            
            // Requisito 5: Mostrar todas las precuentas de la mesa en la vista de pagos
            if (idPedido != null) {
                model.addAttribute("todasLasPrecuentas", precuentaService.listarPrecuentasPorPedido(idPedido));
            }

            return "caja/pago";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Precuenta no encontrada: " + idPrecuenta);
            return "redirect:/caja/terminal";
        });
    }

    /**
     * Formulario de Pago de Pedido Completo (Redirige a precuenta pendiente para evitar cobrar toda la mesa si está dividida)
     */
    @GetMapping("/pago/{idPedido}")
    public String formPago(@PathVariable Integer idPedido, Model model, RedirectAttributes ra) {
        return pedidoService.obtenerPorId(idPedido).map(p -> {
            if (p.getTipo() == Pedido.TipoPedido.DELIVERY) {
                if (p.getEstado() == Pedido.EstadoPedido.PAGADA || (p.getPedidoDelivery() != null && p.getPedidoDelivery().isPagado())) {
                    ra.addFlashAttribute("info", "Este pedido delivery ya se encuentra PAGADO.");
                    Venta v = cajaService.obtenerVentaPorPedido(idPedido).orElse(null);
                    if (v != null) {
                        return "redirect:/caja/comprobante/" + v.getIdVenta();
                    }
                    return "redirect:/caja";
                }
                // Si es delivery pendiente de pago, permitir cobrarlo directamente sin precuenta de mesa
                model.addAttribute("pedido", p);
                model.addAttribute("total", pedidoService.calcularTotal(p));
                model.addAttribute("esPrecuentaDividida", false);
                model.addAttribute("metodosPago", cajaService.listarMetodosPago());
                return "caja/pago";
            }

            if (!despachoService.estaTotalmenteDespachado(idPedido)) {
                String motivo = despachoService.obtenerMotivoBloqueoDespacho(idPedido);
                ra.addFlashAttribute("error", "No se puede cobrar: " + (motivo.isBlank() ? "La comanda aún no ha sido despachada ni entregada a la mesa por el mozo." : motivo));
                return "redirect:/caja/terminal";
            }

            List<Precuenta> pendientes = precuentaService.listarPrecuentasPendientes(idPedido);
            if (!pendientes.isEmpty()) {
                // Redirigir a la precuenta pendiente correspondiente
                return "redirect:/caja/pago/precuenta/" + pendientes.get(0).getIdPrecuenta();
            }

            List<DetallePedido> disponibles = precuentaService.obtenerItemsDisponiblesParaDividir(idPedido);
            if (!disponibles.isEmpty()) {
                ra.addFlashAttribute("error", "Para cobrar, primero debe generar la precuenta seleccionando los productos de la mesa.");
                return "redirect:/caja/terminal";
            }

            ra.addFlashAttribute("info", "Esta comanda ya no tiene precuentas pendientes de cobro.");
            return "redirect:/caja/terminal";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/caja";
        });
    }

    /**
     * Procesar Cobro de una Precuenta Específica
     */
    @PostMapping("/pago/precuenta/{idPrecuenta}")
    public String procesarPagoPrecuenta(@PathVariable Integer idPrecuenta,
                                        @RequestParam(required = false) Integer idMetodoPago,
                                        @RequestParam(required = false) String numeroOperacion,
                                        @RequestParam(required = false) List<Integer> splitMetodoPago,
                                        @RequestParam(required = false) List<String> splitMonto,
                                        @RequestParam(required = false) List<String> splitNumeroOperacion,
                                        @RequestParam(defaultValue = "BOLETA") String tipoComprobante,
                                        @RequestParam(required = false) String numeroComprobante,
                                        @RequestParam(required = false) String nombreCliente,
                                        @RequestParam(required = false) String dniRuc,
                                        HttpSession session,
                                        RedirectAttributes ra) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            List<CajaService.PagoItemDto> medios = procesarMediosDto(idMetodoPago, numeroOperacion, splitMetodoPago, splitMonto, splitNumeroOperacion);

            Venta venta = cajaService.procesarPagoPrecuenta(idPrecuenta, usuario, medios, tipoComprobante, numeroComprobante, nombreCliente, dniRuc);
            String detCliente = (venta.getNombreCliente() != null ? " a nombre de " + venta.getNombreCliente() : "");
            if (venta.getDniRuc() != null && !venta.getDniRuc().isBlank()) {
                detCliente += " (" + venta.getDniRuc() + ")";
            }
            ra.addFlashAttribute("success", "Pago registrado con éxito. Comprobante emitido: " + venta.getNumeroComprobante() + detCliente);
            return "redirect:/caja/comprobante/" + venta.getIdVenta();
        } catch (Exception e) {
            log.error("Error al procesar pago de precuenta", e);
            ra.addFlashAttribute("error", "Error al procesar pago de precuenta: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            return "redirect:/caja/pago/precuenta/" + idPrecuenta;
        }
    }

    /**
     * Procesar Cobro de Pedido Completo
     */
    @PostMapping("/pago/{idPedido}")
    public String procesarPago(@PathVariable Integer idPedido,
                               @RequestParam(required = false) Integer idMetodoPago,
                               @RequestParam(required = false) String numeroOperacion,
                               @RequestParam(required = false) List<Integer> splitMetodoPago,
                               @RequestParam(required = false) List<String> splitMonto,
                               @RequestParam(required = false) List<String> splitNumeroOperacion,
                               @RequestParam(defaultValue = "BOLETA") String tipoComprobante,
                               @RequestParam(required = false) String numeroComprobante,
                               @RequestParam(required = false) String nombreCliente,
                               @RequestParam(required = false) String dniRuc,
                               HttpSession session,
                               RedirectAttributes ra) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            List<CajaService.PagoItemDto> medios = procesarMediosDto(idMetodoPago, numeroOperacion, splitMetodoPago, splitMonto, splitNumeroOperacion);

            Venta venta = cajaService.procesarPagoMulti(idPedido, usuario, medios, tipoComprobante, numeroComprobante, nombreCliente, dniRuc);
            String detCliente = (venta.getNombreCliente() != null ? " a nombre de " + venta.getNombreCliente() : "");
            if (venta.getDniRuc() != null && !venta.getDniRuc().isBlank()) {
                detCliente += " (" + venta.getDniRuc() + ")";
            }
            ra.addFlashAttribute("success", "Pago de comanda registrado con éxito. Comprobante: " + venta.getNumeroComprobante() + detCliente);
            return "redirect:/caja/comprobante/" + venta.getIdVenta();
        } catch (Exception e) {
            log.error("Error al procesar cobro", e);
            ra.addFlashAttribute("error", "Error al procesar cobro: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            return "redirect:/caja/pago/" + idPedido;
        }
    }

    /**
     * Visualizar Comprobante de Venta Emitido (Boleta / Factura)
     */
    @GetMapping("/comprobante/{idVenta}")
    public String verComprobante(@PathVariable Integer idVenta, Model model, RedirectAttributes ra) {
        Venta venta = cajaService.obtenerVentaPorId(idVenta).orElse(null);
        if (venta == null) {
            ra.addFlashAttribute("error", "Comprobante de venta no encontrado (ID: " + idVenta + ")");
            return "redirect:/caja";
        }
        model.addAttribute("venta", venta);
        return "caja/comprobante";
    }

    /**
     * Ver Comprobante emitido a partir del ID de una Precuenta pagada
     */
    @GetMapping("/comprobante/precuenta/{idPrecuenta}")
    public String verComprobantePorPrecuenta(@PathVariable Integer idPrecuenta, RedirectAttributes ra) {
        Venta venta = cajaService.obtenerVentaPorPrecuenta(idPrecuenta).orElse(null);
        if (venta == null) {
            ra.addFlashAttribute("error", "No se encontró un comprobante de venta emitido para la precuenta #" + idPrecuenta);
            return "redirect:/caja";
        }
        return "redirect:/caja/comprobante/" + venta.getIdVenta();
    }

    /**
     * Ver Comprobante emitido a partir del ID de Pedido (especialmente útil para Delivery)
     */
    @GetMapping("/comprobante/pedido/{idPedido}")
    public String verComprobantePorPedido(@PathVariable Integer idPedido, RedirectAttributes ra) {
        Venta venta = cajaService.obtenerVentaPorPedido(idPedido).orElse(null);
        if (venta == null) {
            ra.addFlashAttribute("error", "No se encontró un comprobante emitido para el pedido #" + idPedido);
            return "redirect:/caja";
        }
        return "redirect:/caja/comprobante/" + venta.getIdVenta();
    }

    private List<CajaService.PagoItemDto> procesarMediosDto(Integer idMetodoPago,
                                                            String numeroOperacion,
                                                            List<Integer> splitMetodoPago,
                                                            List<String> splitMonto,
                                                            List<String> splitNumeroOperacion) {
        List<CajaService.PagoItemDto> medios = new ArrayList<>();

        if (splitMetodoPago != null && !splitMetodoPago.isEmpty()) {
            for (int i = 0; i < splitMetodoPago.size(); i++) {
                String strMonto = (splitMonto != null && i < splitMonto.size()) ? splitMonto.get(i) : null;
                BigDecimal monto = parseMontoSafe(strMonto);
                String op = (splitNumeroOperacion != null && i < splitNumeroOperacion.size()) ? splitNumeroOperacion.get(i) : null;
                if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
                    medios.add(new CajaService.PagoItemDto(splitMetodoPago.get(i), monto, op));
                }
            }
        }

        // Si no hubo split con montos positivos, usar el método simple
        if (medios.isEmpty() && idMetodoPago != null) {
            medios.add(new CajaService.PagoItemDto(idMetodoPago, BigDecimal.ZERO, numeroOperacion));
        }

        return medios;
    }

    private BigDecimal parseMontoSafe(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @GetMapping("/cierre")
    public String formCierre(Model model) {
        model.addAttribute("cajaAbierta", cajaService.obtenerCajaAbierta().orElse(null));
        model.addAttribute("resumenTurno", cajaService.obtenerResumenTurno());
        model.addAttribute("cierres", cajaService.listarCierres());
        model.addAttribute("totalGastosHoy", gastoService.totalGastosHoy());
        return "caja/cierre";
    }

    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam BigDecimal montoApertura, HttpSession session, RedirectAttributes ra) {
        try {
            Usuario u = (Usuario) session.getAttribute("usuario");
            cajaService.abrirCaja(u, montoApertura);
            ra.addFlashAttribute("success", "Caja abierta con éxito");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/caja/cierre";
    }

    @PostMapping("/cerrar")
    public String cerrarCaja(@RequestParam(required = false) BigDecimal efectivoReal,
                             RedirectAttributes ra) {
        try {
            cajaService.cerrarCaja(efectivoReal);
            ra.addFlashAttribute("success", "Caja cerrada y arqueada con éxito");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/caja/cierre";
    }
}
