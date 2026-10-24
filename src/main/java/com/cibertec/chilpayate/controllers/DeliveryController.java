package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.model.cart.CarritoDelivery;
import com.cibertec.chilpayate.model.cart.ItemCarrito;
import com.cibertec.chilpayate.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    private final PedidoService pedidoService;
    private final CategoriaService categoriaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final CremaService cremaService;
    private final CarritoService carritoService;
    private final CajaService cajaService;
    private final UsuarioService usuarioService;
    private final ParametrosCartaService parametrosCartaService;

    public DeliveryController(PedidoService pedidoService,
                              CategoriaService categoriaService,
                              ProductoService productoService,
                              ClienteService clienteService,
                              CremaService cremaService,
                              CarritoService carritoService,
                              CajaService cajaService,
                              UsuarioService usuarioService,
                              ParametrosCartaService parametrosCartaService) {
        this.pedidoService = pedidoService;
        this.categoriaService = categoriaService;
        this.productoService = productoService;
        this.clienteService = clienteService;
        this.cremaService = cremaService;
        this.carritoService = carritoService;
        this.cajaService = cajaService;
        this.usuarioService = usuarioService;
        this.parametrosCartaService = parametrosCartaService;
    }


    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "TODOS") String filtro, Model model) {
        List<PedidoDelivery> todos = pedidoService.listarTodosDelivery();

        long pendientesPago = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.PENDIENTE_PAGO).count();
        long enCocina = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.EN_PREPARACION).count();
        long listos = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.LISTO).count();
        long enCamino = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.EN_CAMINO).count();
        long entregados = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.ENTREGADO).count();
        long anulados = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.ANULADO).count();

        List<PedidoDelivery> envios;
        if ("PENDIENTE_PAGO".equalsIgnoreCase(filtro)) {
            envios = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.PENDIENTE_PAGO).toList();
        } else if ("EN_PREPARACION".equalsIgnoreCase(filtro) || "EN_COCINA".equalsIgnoreCase(filtro)) {
            envios = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.EN_PREPARACION).toList();
        } else if ("LISTO".equalsIgnoreCase(filtro)) {
            envios = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.LISTO).toList();
        } else if ("EN_CAMINO".equalsIgnoreCase(filtro)) {
            envios = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.EN_CAMINO).toList();
        } else if ("ENTREGADO".equalsIgnoreCase(filtro)) {
            envios = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.ENTREGADO).toList();
        } else if ("ANULADO".equalsIgnoreCase(filtro)) {
            envios = todos.stream().filter(e -> e.getEstado() == PedidoDelivery.EstadoDelivery.ANULADO).toList();
        } else {
            envios = todos;
        }

        model.addAttribute("envios", envios);
        model.addAttribute("totalEnvios", todos.size());
        model.addAttribute("pendientesPago", pendientesPago);
        model.addAttribute("enCocina", enCocina);
        model.addAttribute("listos", listos);
        model.addAttribute("enCamino", enCamino);
        model.addAttribute("entregados", entregados);
        model.addAttribute("anulados", anulados);
        model.addAttribute("filtroActual", filtro.toUpperCase());
        model.addAttribute("metodosPago", cajaService.listarMetodosPago());

        return "delivery/list";
    }

  
    @GetMapping("/create")
    public String createForm(Model model, HttpSession session) {
        CarritoDelivery carrito = carritoService.obtenerCarritoDelivery(session);

        model.addAttribute("categorias", categoriaService.listarActivos());
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("cremas", cremaService.listarActivos());
        model.addAttribute("salsas", productoService.listarSalsas());
        model.addAttribute("carrito", carrito);
        model.addAttribute("tienePrincipalActivo", carrito != null && carrito.tienePlatoPrincipal());
        return "delivery/create";
    }

    @GetMapping("/nuevo")
    public String nuevoAlias() {
        return "redirect:/delivery/create";
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public Map<String, Object> agregarAlCarritoDelivery(
            @RequestParam Integer idProducto,
            @RequestParam(required = false) List<Integer> idCremas,
            @RequestParam(required = false) List<Integer> idSalsas,
            HttpSession session) {
        try {
            Producto prod = productoService.obtenerPorId(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));

            if (prod.esExtra()) {
                CarritoDelivery carritoActual = carritoService.obtenerCarritoDelivery(session);
                int alitas = carritoActual.contarAlitas();
                int maxPorAlita = parametrosCartaService.getMaxExtrasSalsasPorAlita();
                int permitidas = alitas * maxPorAlita;

                if (alitas == 0) {
                    return Map.of("success", false, "error",
                            "No se puede agregar extras en salsas sin haber seleccionado al menos una porción de alitas.");
                }
                if (carritoActual.contarExtrasSalsas() >= permitidas) {
                    return Map.of("success", false, "error",
                            "Límite alcanzado: Tienes " + alitas + " porción(es) de alitas, que permite como máximo " +
                            permitidas + " extras en salsas (" + maxPorAlita + " por porción).");
                }
            }

            if (prod.esAdicional()) {
                CarritoDelivery carritoActual = carritoService.obtenerCarritoDelivery(session);
                int principales = carritoActual.contarPlatosPrincipales();
                int maxPorPrincipal = parametrosCartaService.getMaxAdicionalesPorPlatoPrincipal();
                int permitidos = principales * maxPorPrincipal;

                if (principales == 0) {
                    return Map.of("success", false, "error",
                            "No se puede agregar adicionales sin haber seleccionado al menos un plato principal.");
                }
                if (carritoActual.contarAdicionales() >= permitidos) {
                    return Map.of("success", false, "error",
                            "Límite alcanzado: Tienes " + principales + " plato(s) principal(es), que permite como máximo " +
                            permitidos + " adicionales (" + maxPorPrincipal + " por plato).");
                }
            }


            if (Boolean.TRUE.equals(prod.getControlaStock()) && prod.getStock() != null) {
                int yaEnCarrito = carritoService.contarPorProductoDelivery(session, idProducto);
                if (yaEnCarrito >= prod.getStock()) {
                    return Map.of("success", false, "error",
                            "Stock insuficiente para '" + prod.getNombre() + "' (Stock disponible: " + prod.getStock() + ")");
                }
            }


            boolean esAlitas = (prod.getCategoria() != null && prod.getCategoria().getNombre().equalsIgnoreCase("ALITAS"))
                    || prod.getNombre().toLowerCase().contains("alita");
            int limiteSalsa = prod.getLimiteSalsa() != null ? prod.getLimiteSalsa() : 0;

            List<String> nombresSalsas = new ArrayList<>();
            if ((esAlitas || Boolean.TRUE.equals(prod.getPermiteSalsa())) && limiteSalsa > 0) {
                if (idSalsas == null || idSalsas.size() != limiteSalsa) {
                    return Map.of("success", false, "error",
                            "Debe seleccionar exactamente " + limiteSalsa + " salsas para " + prod.getNombre());
                }
            }
            if (idSalsas != null) {
                for (Integer idS : idSalsas) {
                    productoService.obtenerPorId(idS).ifPresent(s -> nombresSalsas.add(s.getNombre()));
                }
            }

            List<Integer> finalIdCremas = new ArrayList<>();
            List<String> nombresCremas = new ArrayList<>();
            if (Boolean.TRUE.equals(prod.getPermiteCrema()) && idCremas != null) {
                for (Integer idC : idCremas) {
                    cremaService.obtenerPorId(idC).ifPresent(c -> {
                        finalIdCremas.add(c.getIdCrema());
                        nombresCremas.add(c.getNombre());
                    });
                }
            }

            carritoService.agregarItemDelivery(session, prod, finalIdCremas, nombresCremas, idSalsas, nombresSalsas, "");
            CarritoDelivery carrito = carritoService.obtenerCarritoDelivery(session);

            return Map.of(
                    "success", true,
                    "producto", prod.getNombre(),
                    "cantidadTotal", carrito.getCantidadTotal(),
                    "subtotal", carrito.getSubtotal(),
                    "total", carrito.getTotal(),
                    "tienePrincipal", carrito.tienePlatoPrincipal()
            );

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Error inesperado");
        }
    }

    @GetMapping("/carrito")
    public String verCarritoDelivery(Model model, HttpSession session) {
        CarritoDelivery carrito = carritoService.obtenerCarritoDelivery(session);

        int maxExtrasPorAlita = parametrosCartaService.getMaxExtrasSalsasPorAlita();
        int maxAdicionalesPorPrincipal = parametrosCartaService.getMaxAdicionalesPorPlatoPrincipal();

        int totalAlitas = carrito != null ? carrito.contarAlitas() : 0;
        int totalExtrasSalsas = carrito != null ? carrito.contarExtrasSalsas() : 0;
        int maxSalsasPermitidas = totalAlitas * maxExtrasPorAlita;
        int excesoSalsas = carrito != null ? carrito.calcularExcesoSalsas(maxExtrasPorAlita) : 0;

        int totalPrincipales = carrito != null ? carrito.contarPlatosPrincipales() : 0;
        int totalAdicionales = carrito != null ? carrito.contarAdicionales() : 0;
        int maxAdicionalesPermitidos = totalPrincipales * maxAdicionalesPorPrincipal;
        int excesoAdicionales = carrito != null ? carrito.calcularExcesoAdicionales(maxAdicionalesPorPrincipal) : 0;

        boolean tieneExcesos = excesoSalsas > 0 || excesoAdicionales > 0;

        model.addAttribute("carrito", carrito);
        model.addAttribute("tienePrincipalActivo", carrito != null && carrito.tienePlatoPrincipal());
        model.addAttribute("tieneAdicionales", carrito != null && carrito.tieneAdicionales());
        model.addAttribute("tieneExtrasSalsas", carrito != null && carrito.tieneExtrasSalsas());
        model.addAttribute("clientes", Collections.emptyList());
        model.addAttribute("metodosPago", cajaService.listarMetodosPago());

        model.addAttribute("maxExtrasPorAlita", maxExtrasPorAlita);
        model.addAttribute("maxAdicionalesPorPrincipal", maxAdicionalesPorPrincipal);
        model.addAttribute("totalAlitas", totalAlitas);
        model.addAttribute("totalExtrasSalsas", totalExtrasSalsas);
        model.addAttribute("maxSalsasPermitidas", maxSalsasPermitidas);
        model.addAttribute("excesoSalsas", excesoSalsas);
        model.addAttribute("totalPrincipales", totalPrincipales);
        model.addAttribute("totalAdicionales", totalAdicionales);
        model.addAttribute("maxAdicionalesPermitidos", maxAdicionalesPermitidos);
        model.addAttribute("excesoAdicionales", excesoAdicionales);
        model.addAttribute("tieneExcesos", tieneExcesos);
        return "delivery/carrito";
    }

    /**
     * Endpoint asíncrono para búsqueda rápida de clientes (evita colapso por volumen)
     */
    @GetMapping("/api/clientes/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarClientesAjax(@RequestParam(value = "q", required = false) String q) {
        if (q == null || q.trim().length() < 2) {
            return Collections.emptyList();
        }
        List<Cliente> encontrados = clienteService.buscar(q.trim());
        return encontrados.stream()
                .filter(c -> c.getEstado() == null || c.getEstado())
                .limit(10)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idCliente", c.getIdCliente());
                    map.put("nombre", c.getNombre() != null ? c.getNombre() : "");
                    map.put("telefono", c.getTelefono() != null ? c.getTelefono() : "");
                    map.put("direccion", c.getDireccion() != null ? c.getDireccion() : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/cart/{idItem}/eliminar")
    public String eliminarItemDelivery(@PathVariable String idItem, HttpSession session, RedirectAttributes ra) {
        carritoService.eliminarItemDelivery(session, idItem);
        ra.addFlashAttribute("success", "Plato eliminado del carrito delivery");
        return "redirect:/delivery/carrito";
    }

    @PostMapping("/cart/{idItem}/nota")
    @ResponseBody
    public Map<String, Object> actualizarNotaDelivery(@PathVariable String idItem,
                                                      @RequestParam String nota,
                                                      HttpSession session) {
        carritoService.actualizarNotaDelivery(session, idItem, nota);
        return Map.of("success", true, "idItem", idItem, "nota", nota);
    }

    @RequestMapping(value = "/carrito/cancelar", method = {RequestMethod.GET, RequestMethod.POST})
    public String cancelarCarritoDelivery(HttpSession session, RedirectAttributes ra) {
        carritoService.limpiarCarritoDelivery(session);
        ra.addFlashAttribute("info", "Pedido delivery cancelado.");
        return "redirect:/delivery";
    }


    @PostMapping("/carrito/confirmar")
    public String confirmarPedidoDelivery(
            @RequestParam String nombreCliente,
            @RequestParam(required = false) String telefono,
            @RequestParam String direccion,
            @RequestParam(required = false) String referencia,
            @RequestParam(defaultValue = "5.00") BigDecimal costoDelivery,
            @RequestParam(required = false) String repartidor,
            @RequestParam(required = false) Integer idMetodoPago,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String numeroOperacion,
            @RequestParam(defaultValue = "true") Boolean confirmarPagoAhora,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            CarritoDelivery carrito = carritoService.obtenerCarritoDelivery(session);
            if (carrito.estaVacio()) {
                ra.addFlashAttribute("error", "El carrito de delivery está vacío. Agregue platos antes de confirmar.");
                return "redirect:/delivery/carrito";
            }

            int maxExtrasPorAlita = parametrosCartaService.getMaxExtrasSalsasPorAlita();
            int maxAdicionalesPorPrincipal = parametrosCartaService.getMaxAdicionalesPorPlatoPrincipal();

            int excesoSalsas = carrito.calcularExcesoSalsas(maxExtrasPorAlita);
            int excesoAdicionales = carrito.calcularExcesoAdicionales(maxAdicionalesPorPrincipal);

            if (excesoSalsas > 0 || excesoAdicionales > 0) {
                StringBuilder err = new StringBuilder("No se puede enviar a cocina debido a exceso en el pedido delivery: ");
                if (excesoSalsas > 0) {
                    err.append(excesoSalsas).append(" salsa(s) en exceso (permitidas: ")
                       .append(carrito.contarAlitas() * maxExtrasPorAlita).append(" para ")
                       .append(carrito.contarAlitas()).append(" porción(es) de alitas). ");
                }
                if (excesoAdicionales > 0) {
                    err.append(excesoAdicionales).append(" adicional(es) en exceso (permitidos: ")
                       .append(carrito.contarPlatosPrincipales() * maxAdicionalesPorPrincipal).append(" para ")
                       .append(carrito.contarPlatosPrincipales()).append(" plato(s) principal(es)).");
                }
                ra.addFlashAttribute("error", err.toString());
                return "redirect:/delivery/carrito";
            }

            boolean tienePrincipalEnCarrito = carrito.tienePlatoPrincipal();
            boolean tieneAdicionales = carrito.getItems().stream().anyMatch(i -> i.getTipo() == com.cibertec.chilpayate.model.TipoProducto.ADICIONAL);
            if (tieneAdicionales && !tienePrincipalEnCarrito) {
                ra.addFlashAttribute("error", "El pedido no puede contener solo adicionales. Debe incluir al menos un plato principal.");
                return "redirect:/delivery/carrito";
            }

            if (nombreCliente == null || nombreCliente.isBlank()) {
                throw new IllegalArgumentException("El nombre del cliente es obligatorio");
            }
            if (direccion == null || direccion.isBlank()) {
                throw new IllegalArgumentException("La dirección de entrega es obligatoria");
            }

            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                usuario = usuarioService.obtenerPorUsername("admin").orElse(null);
            }

            Cliente cliente = null;
            if (telefono != null && !telefono.isBlank()) {
                List<Cliente> encontrados = clienteService.buscar(telefono.trim());
                if (!encontrados.isEmpty()) {
                    cliente = encontrados.get(0);
                }
            }
            if (cliente == null) {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setNombre(nombreCliente.trim());
                nuevoCliente.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : "-");
                nuevoCliente.setDireccion(direccion.trim());
                nuevoCliente.setEstado(true);
                cliente = clienteService.guardar(nuevoCliente);
            } else {
                if (direccion != null && !direccion.isBlank()) {
                    cliente.setDireccion(direccion.trim());
                    clienteService.actualizar(cliente.getIdCliente(), cliente);
                }
            }

            Pedido pedido = pedidoService.crearPedidoDelivery(
                    usuario,
                    cliente,
                    direccion.trim(),
                    telefono != null ? telefono.trim() : "",
                    referencia != null ? referencia.trim() : "",
                    costoDelivery != null ? costoDelivery : BigDecimal.ZERO,
                    repartidor != null && !repartidor.isBlank() ? repartidor.trim() : "Por asignar");

            PedidoDelivery del = pedido.getPedidoDelivery();
            if (del == null) {
                del = pedidoService.obtenerDeliveryPorPedido(pedido.getIdPedido()).orElse(null);
            }

            if (del != null) {
                if (Boolean.TRUE.equals(confirmarPagoAhora)) {
                    del.setEstado(PedidoDelivery.EstadoDelivery.EN_PREPARACION);
                } else {
                    del.setEstado(PedidoDelivery.EstadoDelivery.PENDIENTE_PAGO);
                }
                pedidoService.guardarDelivery(del);
            }

            for (ItemCarrito it : carrito.getItems()) {
                pedidoService.agregarItem(
                        pedido.getIdPedido(),
                        it.getIdProducto(),
                        it.getIdCremas(),
                        it.getIdSalsas(),
                        it.getNota()
                );
            }

            Integer idMp = idMetodoPago;
            if (idMp == null && metodoPago != null && !metodoPago.isBlank()) {
                List<MetodoPago> mps = cajaService.listarMetodosPago().stream()
                        .filter(mp -> mp.getNombre() != null && mp.getNombre().toLowerCase().contains(metodoPago.trim().toLowerCase()))
                        .toList();
                if (!mps.isEmpty()) {
                    idMp = mps.get(0).getIdMetodoPago();
                }
            }
            if (idMp == null) {
                List<MetodoPago> mps = cajaService.listarMetodosPago();
                if (!mps.isEmpty()) {
                    idMp = mps.get(0).getIdMetodoPago();
                }
            }

            if (Boolean.TRUE.equals(confirmarPagoAhora)) {
                // 1. Enviar primero a cocina para registrar TicketCocina y pasar platos a EN_COCINA
                pedidoService.enviarACocina(pedido.getIdPedido());

                // 2. Procesar pago adelantado y emitir comprobante de venta
                Venta ventaEmitida = null;
                try {
                    ventaEmitida = cajaService.procesarPagoTotal(pedido.getIdPedido(), usuario, idMp, numeroOperacion, "BOLETA", null);
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(DeliveryController.class).error("Error al procesar pago adelantado delivery", ex);
                }

                String compMsg = (ventaEmitida != null)
                        ? " Comprobante: " + ventaEmitida.getNumeroComprobante() + " emitido."
                        : "";
                ra.addFlashAttribute("success",
                        "🛵 Pedido Delivery registrado con PAGO ADELANTADO CONFIRMADO." + compMsg + " Comanda enviada a cocina. Código: " + pedido.getCodigo());
            } else {
                ra.addFlashAttribute("warn",
                        "⏳ Pedido Delivery registrado en ESPERA DE PAGO. Se enviará a cocina y se generará la Venta cuando el cliente confirme el abono.");
            }

            carritoService.limpiarCarritoDelivery(session);
            return "redirect:/delivery";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al registrar pedido delivery: " + e.getMessage());
            return "redirect:/delivery/carrito";
        }
    }


    @PostMapping("/{id}/confirmar-pago")
    public String confirmarPagoYEnviarACocina(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer idMetodoPago,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String numeroOperacion,
            HttpSession session,
            RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresentOrElse(d -> {
            try {
                Pedido p = d.getPedido();

                Integer idMp = idMetodoPago;
                if (idMp == null && metodoPago != null && !metodoPago.isBlank()) {
                    List<MetodoPago> mps = cajaService.listarMetodosPago().stream()
                            .filter(mp -> mp.getNombre() != null && mp.getNombre().toLowerCase().contains(metodoPago.trim().toLowerCase()))
                            .toList();
                    if (!mps.isEmpty()) {
                        idMp = mps.get(0).getIdMetodoPago();
                    }
                }
                if (idMp == null) {
                    List<MetodoPago> mps = cajaService.listarMetodosPago();
                    if (!mps.isEmpty()) {
                        idMp = mps.get(0).getIdMetodoPago();
                    }
                }

                Usuario usuario = (Usuario) session.getAttribute("usuario");
                if (usuario == null) {
                    usuario = usuarioService.obtenerPorUsername("admin").orElse(null);
                }

                // 1. Enviar comanda a cocina
                pedidoService.enviarACocina(p.getIdPedido());

                // 2. Registrar cobro de venta adelantada
                Venta ventaEmitida = cajaService.procesarPagoTotal(p.getIdPedido(), usuario, idMp, numeroOperacion, "BOLETA", null);

                d.setEstado(PedidoDelivery.EstadoDelivery.EN_PREPARACION);
                pedidoService.guardarDelivery(d);

                ra.addFlashAttribute("success",
                        "💳 Pago confirmado y Comprobante " + ventaEmitida.getNumeroComprobante() + " emitido. Pedido " + p.getCodigo() + " en preparación en cocina.");
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Error al confirmar pago: " + e.getMessage());
            }
        }, () -> ra.addFlashAttribute("error", "Pedido delivery no encontrado."));

        return "redirect:/delivery";
    }

    @PostMapping("/{id}/anular")
    public String anularPedidoDelivery(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "No se confirmó el pago en tiempo prudente") String motivo,
            RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresentOrElse(d -> {
            try {
                pedidoService.anularPedido(d.getPedido().getIdPedido(), motivo);
                d.setEstado(PedidoDelivery.EstadoDelivery.ANULADO);
                pedidoService.guardarDelivery(d);
                ra.addFlashAttribute("info",
                        "✕ Pedido " + d.getPedido().getCodigo() + " anulado exitosamente. Stock devuelto a inventario.");
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Error al anular pedido delivery: " + e.getMessage());
            }
        }, () -> ra.addFlashAttribute("error", "Pedido delivery no encontrado."));

        return "redirect:/delivery";
    }

    @PostMapping("/{id}/marcar-listo")
    public String marcarPlatosListos(@PathVariable Integer id, RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresent(d -> {
            d.setEstado(PedidoDelivery.EstadoDelivery.LISTO);
            pedidoService.guardarDelivery(d);
            ra.addFlashAttribute("success", "📦 Pedido " + d.getPedido().getCodigo() + " marcado como LISTO en barra. Empacar y entregar al repartidor.");
        });
        return "redirect:/delivery";
    }

    @PostMapping("/{id}/despachar")
    public String despacharAlRepartidor(
            @PathVariable Integer id,
            @RequestParam String repartidor,
            RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresent(d -> {
            d.setRepartidor(repartidor);
            d.setEstado(PedidoDelivery.EstadoDelivery.EN_CAMINO);
            pedidoService.guardarDelivery(d);
            ra.addFlashAttribute("success", "🛵 Pedido " + d.getPedido().getCodigo() + " entregado al repartidor '" + repartidor + "' y en camino.");
        });
        return "redirect:/delivery";
    }

    @PostMapping("/{id}/marcar-entregado")
    public String marcarEntregado(@PathVariable Integer id, RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresent(d -> {
            d.setEstado(PedidoDelivery.EstadoDelivery.ENTREGADO);
            d.setFechaEntrega(LocalDateTime.now());
            pedidoService.guardarDelivery(d);
            ra.addFlashAttribute("success", "✅ Pedido " + d.getPedido().getCodigo() + " completado y entregado con éxito.");
        });
        return "redirect:/delivery";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Integer id,
                                @RequestParam PedidoDelivery.EstadoDelivery nuevoEstado,
                                RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresent(d -> {
            d.setEstado(nuevoEstado);
            if (nuevoEstado == PedidoDelivery.EstadoDelivery.ENTREGADO) {
                d.setFechaEntrega(LocalDateTime.now());
            }
            pedidoService.guardarDelivery(d);
            ra.addFlashAttribute("success", "Estado de delivery actualizado a: " + nuevoEstado);
        });
        return "redirect:/delivery";
    }

    @PostMapping("/{id}/repartidor")
    public String asignarRepartidor(@PathVariable Integer id,
                                    @RequestParam String repartidor,
                                    RedirectAttributes ra) {
        pedidoService.obtenerDeliveryPorId(id).ifPresent(d -> {
            d.setRepartidor(repartidor);
            pedidoService.guardarDelivery(d);
            ra.addFlashAttribute("success", "Repartidor '" + repartidor + "' asignado al pedido.");
        });
        return "redirect:/delivery";
    }
}
