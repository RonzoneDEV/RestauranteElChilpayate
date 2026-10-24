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
import java.util.List;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final MesaService mesaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final CremaService cremaService;
    private final PrecuentaService precuentaService;
    private final CategoriaService categoriaService;
    private final CarritoService carritoService;
    private final ParametrosCartaService parametrosCartaService;

    public PedidoController(PedidoService pedidoService,
                            MesaService mesaService,
                            ClienteService clienteService,
                            ProductoService productoService,
                            CremaService cremaService,
                            PrecuentaService precuentaService,
                            CategoriaService categoriaService,
                            CarritoService carritoService,
                            ParametrosCartaService parametrosCartaService) {
        this.pedidoService = pedidoService;
        this.mesaService = mesaService;
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.cremaService = cremaService;
        this.precuentaService = precuentaService;
        this.categoriaService = categoriaService;
        this.carritoService = carritoService;
        this.parametrosCartaService = parametrosCartaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pedidos", pedidoService.listarActivos());
        return "pedidos/list";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam(required = false) Integer idMesa, Model model) {
        model.addAttribute("mesas", mesaService.listarLibres());
        model.addAttribute("clientes", clienteService.listarActivos());
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("cremas", cremaService.listarActivos());
        model.addAttribute("salsas", productoService.listarSalsas());
        model.addAttribute("idMesaSeleccionada", idMesa);
        return "pedidos/create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String tipo,
                         @RequestParam(required = false) Integer idMesa,
                         @RequestParam(required = false) Integer idCliente,
                         @RequestParam(required = false) String direccion,
                         @RequestParam(required = false) String telefono,
                         @RequestParam(required = false) String referencia,
                         @RequestParam(required = false) BigDecimal costoDelivery,
                         @RequestParam(required = false) String repartidor,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Cliente cliente = idCliente != null ? clienteService.obtenerPorId(idCliente).orElse(null) : null;

            Pedido nuevo;
            if ("DELIVERY".equalsIgnoreCase(tipo)) {
                nuevo = pedidoService.crearPedidoDelivery(usuario, cliente, direccion, telefono, referencia, costoDelivery, repartidor);
            } else {
                if (idMesa == null) {
                    throw new IllegalArgumentException("Debe seleccionar una mesa para pedidos de salón");
                }
                nuevo = pedidoService.crearPedidoSalon(idMesa, usuario, cliente, null);
            }

            ra.addFlashAttribute("success", "Pedido creado con código: " + nuevo.getCodigo());
            return "redirect:/pedidos/edit/" + nuevo.getIdPedido();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/create";
        }
    }

    @GetMapping("/mesa/{idMesa}")
    public String pedidoMesa(@PathVariable Integer idMesa, Model model, HttpSession session, RedirectAttributes ra) {
        Mesa mesa = mesaService.obtenerPorId(idMesa).orElse(null);
        if (mesa == null) {
            ra.addFlashAttribute("error", "Mesa no encontrada: " + idMesa);
            return "redirect:/salon";
        }

        if (mesa.getEstado() == Mesa.EstadoMesa.BLOQUEADA) {
            ra.addFlashAttribute("error", "⛔ Acceso bloqueado: La Mesa #" + mesa.getNumero() + " está BLOQUEADA por precuenta en cobro. No se permite agregar más productos.");
            return "redirect:/salon";
        }

        // Al ingresar a pedidos, la mesa se actualiza a OCUPADA
        if (mesa.getEstado() == Mesa.EstadoMesa.LIBRE) {
            mesa.setEstado(Mesa.EstadoMesa.OCUPADA);
            mesaService.guardar(mesa);
        }

        Pedido pedidoActivo = pedidoService.obtenerPorMesaActiva(idMesa).orElse(null);
        com.cibertec.chilpayate.model.cart.CarritoMesa carrito = carritoService.obtenerCarrito(session, mesa);

        boolean tienePrincipalEnPedido = pedidoActivo != null && pedidoService.tienePlatoPrincipal(pedidoActivo);
        boolean tienePrincipalEnCarrito = carrito != null && carrito.tienePlatoPrincipal();

        model.addAttribute("mesa", mesa);
        model.addAttribute("pedido", pedidoActivo);
        model.addAttribute("pedidoActivo", pedidoActivo);
        model.addAttribute("carrito", carrito);
        model.addAttribute("tienePrincipalActivo", tienePrincipalEnPedido || tienePrincipalEnCarrito);
        model.addAttribute("categorias", categoriaService.listarActivos());
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("salsas", productoService.listarSalsas());
        return "pedidos/pos";
    }

    @GetMapping("/mesa/{idMesa}/volver")
    public String volverAlSalon(@PathVariable Integer idMesa, HttpSession session, RedirectAttributes ra) {
        try {
            java.util.Optional<Pedido> pedidoOpt = pedidoService.obtenerPorMesaActiva(idMesa);
            if (pedidoOpt.isEmpty()) {
                // Al salir de la pantalla sin comanda activa confirmada, la mesa vuelve a LIBRE
                carritoService.limpiarCarrito(session, idMesa);
                mesaService.cambiarEstado(idMesa, Mesa.EstadoMesa.LIBRE);
                ra.addFlashAttribute("info", "Mesa #" + idMesa + " liberada.");
            }
        } catch (Exception e) {
            // Silencioso
        }
        return "redirect:/salon";
    }

    @PostMapping("/mesa/{idMesa}/cart/add")
    @ResponseBody
    public java.util.Map<String, Object> agregarAlCarritoAjax(
            @PathVariable Integer idMesa,
            @RequestParam Integer idProducto,
            @RequestParam(required = false) List<Integer> idSalsas,
            HttpSession session) {
        try {
            Mesa mesa = mesaService.obtenerPorId(idMesa)
                    .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
            if (mesa.getEstado() == Mesa.EstadoMesa.BLOQUEADA) {
                return java.util.Map.of("success", false, "error",
                        "La Mesa #" + mesa.getNumero() + " está BLOQUEADA por precuenta en cobro. No se permite agregar productos.");
            }
            Producto prod = productoService.obtenerPorId(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            // Regla de Extras (Salsas):
            // Salsas se clasifican como TipoProducto.EXTRA y están vinculadas proporcionalmente a las porciones de Alitas
            if (prod.esExtra()) {
                com.cibertec.chilpayate.model.cart.CarritoMesa carritoActual = carritoService.obtenerCarrito(session, mesa);
                int alitas = carritoActual.contarAlitas();
                int maxPorAlita = parametrosCartaService.getMaxExtrasSalsasPorAlita();
                int permitidas = alitas * maxPorAlita;

                if (alitas == 0) {
                    return java.util.Map.of("success", false, "error",
                            "No se puede agregar extras en salsas sin haber seleccionado al menos una porción de alitas.");
                }
                if (carritoActual.contarExtrasSalsas() >= permitidas) {
                    return java.util.Map.of("success", false, "error",
                            "Límite alcanzado: Tienes " + alitas + " porción(es) de alitas, que permite como máximo " +
                            permitidas + " extras en salsas (" + maxPorAlita + " por porción).");
                }
            }

            // Regla de Adicionales Generales:
            // Vinculados proporcionalmente a Platos Principales (incluye Alitas, Hamburguesas, Salchipapas, etc.)
            if (prod.esAdicional()) {
                com.cibertec.chilpayate.model.cart.CarritoMesa carritoActual = carritoService.obtenerCarrito(session, mesa);
                int principales = carritoActual.contarPlatosPrincipales();
                int maxPorPrincipal = parametrosCartaService.getMaxAdicionalesPorPlatoPrincipal();
                int permitidos = principales * maxPorPrincipal;

                if (principales == 0) {
                    return java.util.Map.of("success", false, "error",
                            "No se puede agregar adicionales sin haber seleccionado al menos un plato principal.");
                }
                if (carritoActual.contarAdicionales() >= permitidos) {
                    return java.util.Map.of("success", false, "error",
                            "Límite alcanzado: Tienes " + principales + " plato(s) principal(es), que permite como máximo " +
                            permitidos + " adicionales (" + maxPorPrincipal + " por plato).");
                }
            }

            // Regla de Stock:
            // Si controlaStock == true: seguir reglas de stock
            // Si controlaStock == false: no seguir reglas de stock
            if (Boolean.TRUE.equals(prod.getControlaStock()) && prod.getStock() != null) {
                int yaEnCarrito = carritoService.contarPorProducto(session, idMesa, idProducto);
                if (yaEnCarrito >= prod.getStock()) {
                    return java.util.Map.of("success", false, "error",
                            "Stock insuficiente para '" + prod.getNombre() + "' (Stock disponible: " + prod.getStock() + ")");
                }
            }

            // Regla de Salsas para categoría Alitas:
            // Si el producto permite N salsas, debe seleccionarse exactamente esa cantidad
            boolean esAlitas = (prod.getCategoria() != null && prod.getCategoria().getNombre().equalsIgnoreCase("ALITAS"))
                    || prod.getNombre().toLowerCase().contains("alita");
            int limiteSalsa = prod.getLimiteSalsa() != null ? prod.getLimiteSalsa() : 0;

            List<String> nombresSalsas = new java.util.ArrayList<>();
            if (esAlitas && limiteSalsa > 0) {
                if (idSalsas == null || idSalsas.size() != limiteSalsa) {
                    return java.util.Map.of("success", false, "error",
                            "Debe seleccionar exactamente " + limiteSalsa + " salsas para " + prod.getNombre());
                }
                for (Integer ids : idSalsas) {
                    productoService.obtenerPorId(ids).ifPresent(s -> nombresSalsas.add(s.getNombre()));
                }
            }

            // Cada producto se selecciona UNO A UNO individualmente.
            // Las notas se agregan en el formulario del carrito.
            carritoService.agregarItem(session, mesa, prod, idSalsas, nombresSalsas, "");
            com.cibertec.chilpayate.model.cart.CarritoMesa carrito = carritoService.obtenerCarrito(session, mesa);
            java.util.Optional<Pedido> pedidoActivoOpt = pedidoService.obtenerPorMesaActiva(idMesa);
            boolean tienePrincipal = carrito.tienePlatoPrincipal()
                    || (pedidoActivoOpt.isPresent() && pedidoService.tienePlatoPrincipal(pedidoActivoOpt.get()));

            return java.util.Map.of(
                    "success", true,
                    "producto", prod.getNombre(),
                    "cantidadTotal", carrito.getCantidadTotal(),
                    "total", carrito.getTotal(),
                    "tienePrincipal", tienePrincipal
            );
        } catch (Exception e) {
            return java.util.Map.of("success", false, "error", e.getMessage());
        }
    }

    @GetMapping("/mesa/{idMesa}/carrito")
    public String verCarritoMesa(@PathVariable Integer idMesa, Model model, HttpSession session, RedirectAttributes ra) {
        Mesa mesa = mesaService.obtenerPorId(idMesa).orElse(null);
        if (mesa == null) {
            ra.addFlashAttribute("error", "Mesa no encontrada: " + idMesa);
            return "redirect:/salon";
        }
        Pedido pedidoActivo = pedidoService.obtenerPorMesaActiva(idMesa).orElse(null);
        com.cibertec.chilpayate.model.cart.CarritoMesa carrito = carritoService.obtenerCarrito(session, mesa);

        BigDecimal totalConsumoCocina = BigDecimal.ZERO;
        if (pedidoActivo != null) {
            totalConsumoCocina = pedidoService.calcularTotal(pedidoActivo);
        }
        BigDecimal totalGeneral = carrito.getTotal().add(totalConsumoCocina);

        boolean tienePrincipalEnPedido = pedidoActivo != null && pedidoService.tienePlatoPrincipal(pedidoActivo);
        boolean tienePrincipalEnCarrito = carrito != null && carrito.tienePlatoPrincipal();

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

        model.addAttribute("mesa", mesa);
        model.addAttribute("pedido", pedidoActivo);
        model.addAttribute("pedidoActivo", pedidoActivo);
        model.addAttribute("carrito", carrito);
        model.addAttribute("tienePrincipalActivo", tienePrincipalEnPedido || tienePrincipalEnCarrito);
        model.addAttribute("tieneAdicionales", carrito != null && carrito.tieneAdicionales());
        model.addAttribute("tieneExtrasSalsas", carrito != null && carrito.tieneExtrasSalsas());
        model.addAttribute("totalConsumoCocina", totalConsumoCocina);
        model.addAttribute("totalGeneral", totalGeneral);
        model.addAttribute("salsas", productoService.listarSalsas());

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
        return "pedidos/carrito";
    }

    @PostMapping("/mesa/{idMesa}/cart/{idItem}/eliminar")
    public String eliminarItemCarrito(@PathVariable Integer idMesa, @PathVariable String idItem, HttpSession session, RedirectAttributes ra) {
        carritoService.eliminar(session, idMesa, idItem);
        ra.addFlashAttribute("success", "Plato eliminado del carrito");
        return "redirect:/pedidos/mesa/" + idMesa + "/carrito";
    }

    @PostMapping("/mesa/{idMesa}/cart/{idItem}/nota")
    @ResponseBody
    public java.util.Map<String, Object> actualizarNotaCarrito(
            @PathVariable Integer idMesa, @PathVariable String idItem,
            @RequestParam String nota, HttpSession session) {
        carritoService.actualizarNota(session, idMesa, idItem, nota);
        return java.util.Map.of("success", true, "idItem", idItem, "nota", nota);
    }

    @RequestMapping(value = "/mesa/{idMesa}/carrito/cancelar", method = {RequestMethod.GET, RequestMethod.POST})
    public String cancelarCarritoMesa(@PathVariable Integer idMesa, HttpSession session, RedirectAttributes ra) {
        carritoService.limpiarCarrito(session, idMesa);
        java.util.Optional<Pedido> pedidoOpt = pedidoService.obtenerPorMesaActiva(idMesa);
        if (pedidoOpt.isEmpty()) {
            // Al dar en cancelar sin comanda activa, la mesa vuelve a aparecer como LIBRE
            mesaService.cambiarEstado(idMesa, Mesa.EstadoMesa.LIBRE);
            ra.addFlashAttribute("info", "Pedido cancelado. Mesa #" + idMesa + " liberada.");
        } else {
            ra.addFlashAttribute("info", "Nuevos platos cancelados. La comanda activa de la mesa se mantiene.");
        }
        return "redirect:/salon";
    }

    @PostMapping("/mesa/{idMesa}/carrito/confirmar")
    public String confirmarPedidoCarrito(@PathVariable Integer idMesa, HttpSession session, RedirectAttributes ra) {
        try {
            Mesa mesa = mesaService.obtenerPorId(idMesa)
                    .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
            if (mesa.getEstado() == Mesa.EstadoMesa.BLOQUEADA) {
                ra.addFlashAttribute("error", "⛔ La Mesa #" + mesa.getNumero() + " está BLOQUEADA por precuenta en cobro. No se pueden confirmar nuevos pedidos.");
                return "redirect:/salon";
            }
            com.cibertec.chilpayate.model.cart.CarritoMesa carrito = carritoService.obtenerCarrito(session, mesa);

            if (carrito.estaVacio()) {
                ra.addFlashAttribute("error", "El carrito está vacío. Agregue platos antes de confirmar.");
                return "redirect:/pedidos/mesa/" + idMesa;
            }

            // Validación estricta de excesos de salsas y adicionales proporcionales:
            int maxExtrasPorAlita = parametrosCartaService.getMaxExtrasSalsasPorAlita();
            int maxAdicionalesPorPrincipal = parametrosCartaService.getMaxAdicionalesPorPlatoPrincipal();

            int excesoSalsas = carrito.calcularExcesoSalsas(maxExtrasPorAlita);
            int excesoAdicionales = carrito.calcularExcesoAdicionales(maxAdicionalesPorPrincipal);

            if (excesoSalsas > 0 || excesoAdicionales > 0) {
                StringBuilder err = new StringBuilder("No se puede enviar a cocina debido a exceso en el pedido: ");
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
                return "redirect:/pedidos/mesa/" + idMesa + "/carrito";
            }

            java.util.Optional<Pedido> pedidoOpt = pedidoService.obtenerPorMesaActiva(idMesa);
            boolean tienePrincipalEnCarrito = carrito.tienePlatoPrincipal();
            boolean tienePrincipalEnPedido = pedidoOpt.isPresent() && pedidoService.tienePlatoPrincipal(pedidoOpt.get());
            boolean tieneAdicionales = carrito.getItems().stream().anyMatch(i -> i.getTipo() == com.cibertec.chilpayate.model.TipoProducto.ADICIONAL);

            if (tieneAdicionales && !tienePrincipalEnCarrito && !tienePrincipalEnPedido) {
                ra.addFlashAttribute("error", "El pedido no puede contener solo adicionales. Debe incluir al menos un plato principal.");
                return "redirect:/pedidos/mesa/" + idMesa + "/carrito";
            }

            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Pedido pedido;
            boolean esNuevo = false;

            if (pedidoOpt.isPresent()) {
                pedido = pedidoOpt.get();
            } else {
                pedido = pedidoService.crearPedidoSalon(idMesa, usuario, null, null);
                esNuevo = true;
            }

            // Registrar cada plato individualmente (uno a uno) en DetallePedido con su nota y salsas
            for (com.cibertec.chilpayate.model.cart.ItemCarrito item : carrito.getItems()) {
                pedidoService.agregarItem(pedido.getIdPedido(), item.getIdProducto(),
                        null, item.getIdSalsas(), item.getNota());
            }

            // Enviar a cocina generando TicketCocina
            TicketCocina ticket = pedidoService.enviarACocina(pedido.getIdPedido());

            // Limpiar carrito de sesión
            carritoService.limpiarCarrito(session, idMesa);

            String correlativo = ticket != null ? String.valueOf(ticket.getCorrelativo()) : "1";
            if (esNuevo) {
                ra.addFlashAttribute("success", "¡Comanda " + pedido.getCodigo() + " iniciada! Ticket #" + correlativo + " enviado a Cocina exitosamente.");
            } else {
                ra.addFlashAttribute("success", "¡Nuevos platos agregados a Mesa #" + mesa.getNumero() + "! Ticket #" + correlativo + " enviado a Cocina.");
            }

            return "redirect:/salon";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al procesar el pedido: " + e.getMessage());
            return "redirect:/pedidos/mesa/" + idMesa + "/carrito";
        }
    }

    @GetMapping("/pos/{id}")
    public String posScreen(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return pedidoService.obtenerPorId(id).map(p -> {
            if (p.getMesa() != null) {
                return "redirect:/pedidos/mesa/" + p.getMesa().getIdMesa();
            }
            model.addAttribute("pedido", p);
            model.addAttribute("total", pedidoService.calcularTotal(p));
            model.addAttribute("categorias", categoriaService.listarActivos());
            model.addAttribute("productos", productoService.listarActivos());
            model.addAttribute("cremas", cremaService.listarActivos());
            model.addAttribute("salsas", productoService.listarSalsas());
            model.addAttribute("proximoTicket", pedidoService.obtenerProximoNumeroTicket(id));
            return "pedidos/pos";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/salon";
        });
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id) {
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/edit/{id}/item", "/pos/{id}/item"})
    public String agregarItem(@PathVariable Integer id,
                              @RequestParam Integer idProducto,
                              @RequestParam(required = false) List<Integer> idCremas,
                              @RequestParam(required = false) List<Integer> idSalsas,
                              @RequestParam(required = false) String nota,
                              RedirectAttributes ra) {
        try {
            pedidoService.agregarItem(id, idProducto, idCremas, idSalsas, nota);
            ra.addFlashAttribute("success", "Ítem agregado al borrador");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/edit/{id}/item/{idDetalle}/eliminar", "/pos/{id}/item/{idDetalle}/eliminar"})
    public String eliminarItem(@PathVariable Integer id, @PathVariable Integer idDetalle, RedirectAttributes ra) {
        try {
            pedidoService.eliminarItem(idDetalle);
            ra.addFlashAttribute("success", "Ítem borrador eliminado del carrito");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/edit/{id}/item/{idDetalle}/nota", "/pos/{id}/item/{idDetalle}/nota"})
    public String actualizarNota(@PathVariable Integer id, @PathVariable Integer idDetalle,
                                 @RequestParam String nota, RedirectAttributes ra) {
        try {
            pedidoService.actualizarNota(idDetalle, nota);
            ra.addFlashAttribute("success", "Nota actualizada para cocina");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/edit/{id}/item/{idDetalle}/extras", "/pos/{id}/item/{idDetalle}/extras"})
    public String actualizarExtras(@PathVariable Integer id, @PathVariable Integer idDetalle,
                                   @RequestParam(required = false) List<Integer> idCremas,
                                   @RequestParam(required = false) List<Integer> idSalsas,
                                   RedirectAttributes ra) {
        try {
            pedidoService.actualizarExtras(idDetalle, idCremas, idSalsas);
            ra.addFlashAttribute("success", "Cremas y salsas actualizadas");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/edit/{id}/cocina", "/pos/{id}/cocina"})
    public String enviarCocina(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            TicketCocina ticket = pedidoService.enviarACocina(id);
            if (ticket != null) {
                ra.addFlashAttribute("success", "¡Ticket #" + ticket.getCorrelativo() + " enviado a cocina correctamente!");
            } else {
                ra.addFlashAttribute("info", "No hay ítems en borrador pendientes para enviar a cocina");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/edit/{id}/precuenta", "/pos/{id}/precuenta"})
    public String precuenta(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            precuentaService.generarPrecuenta(id);
            ra.addFlashAttribute("success", "Precuenta generada y enviada a Caja correctamente. Mesa bloqueada para nuevos pedidos.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo generar la precuenta: " + e.getMessage());
        }
        return "redirect:/pedidos/pos/" + id;
    }

    @PostMapping({"/pos/{id}/cancelar", "/edit/{id}/cancelar"})
    public String cancelarPedidoPos(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            java.util.Optional<Pedido> pOpt = pedidoService.obtenerPorId(id);
            if (pOpt.isPresent()) {
                Pedido p = pOpt.get();
                Integer numeroMesa = p.getMesa() != null ? p.getMesa().getNumero() : null;
                precuentaService.eliminarPrecuenta(id);
                pedidoService.anularPedido(id, "Cancelado desde terminal POS");
                if (numeroMesa != null) {
                    ra.addFlashAttribute("success", "Pedido cancelado y Mesa #" + numeroMesa + " liberada exitosamente.");
                } else {
                    ra.addFlashAttribute("success", "Pedido cancelado exitosamente.");
                }
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al cancelar el pedido: " + e.getMessage());
        }
        return "redirect:/salon";
    }

    @GetMapping("/pos/{id}/volver")
    public String volverAlSalon(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            java.util.Optional<Pedido> pOpt = pedidoService.obtenerPorId(id);
            if (pOpt.isPresent()) {
                Pedido p = pOpt.get();
                List<DetallePedido> detalles = p.getDetallePedido();
                boolean sinProductos = (detalles == null || detalles.isEmpty() ||
                        detalles.stream().allMatch(d -> d.getEstado() == DetallePedido.EstadoDetalle.ANULADO));
                if (sinProductos && p.getMesa() != null) {
                    Integer numeroMesa = p.getMesa().getNumero();
                    precuentaService.eliminarPrecuenta(id);
                    pedidoService.anularPedido(id, "Cancelado automáticamente por salir sin productos agregados");
                    ra.addFlashAttribute("info", "Mesa #" + numeroMesa + " liberada automáticamente (sin productos agregados).");
                }
            }
        } catch (Exception e) {
            // Manejo silencioso
        }
        return "redirect:/salon";
    }

    @PostMapping("/anular/{id}")
    public String anular(@PathVariable Integer id, @RequestParam(defaultValue = "Cancelado por el cliente") String motivo, RedirectAttributes ra) {
        try {
            precuentaService.eliminarPrecuenta(id);
            pedidoService.anularPedido(id, motivo);
            ra.addFlashAttribute("success", "Pedido anulado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    // Endpoints REST/AJAX para experiencia UI/UX fluida e instantánea
    @PostMapping("/api/{id}/item")
    @ResponseBody
    public java.util.Map<String, Object> agregarItemAjax(
            @PathVariable Integer id,
            @RequestParam Integer idProducto,
            @RequestParam(required = false) List<Integer> idCremas,
            @RequestParam(required = false) List<Integer> idSalsas,
            @RequestParam(required = false) String nota) {
        try {
            DetallePedido det = pedidoService.agregarItem(id, idProducto, idCremas, idSalsas, nota);
            Pedido p = pedidoService.obtenerPorId(id).orElseThrow();
            return java.util.Map.of(
                    "success", true,
                    "idDetalle", det.getIdDetallePedido(),
                    "producto", det.getProducto().getNombre(),
                    "precio", det.getPrecioActual(),
                    "total", pedidoService.calcularTotal(p)
            );
        } catch (Exception e) {
            return java.util.Map.of("success", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/{id}/item/{idDetalle}/eliminar")
    @ResponseBody
    public java.util.Map<String, Object> eliminarItemAjax(@PathVariable Integer id, @PathVariable Integer idDetalle) {
        try {
            pedidoService.eliminarItem(idDetalle);
            Pedido p = pedidoService.obtenerPorId(id).orElseThrow();
            return java.util.Map.of("success", true, "total", pedidoService.calcularTotal(p));
        } catch (Exception e) {
            return java.util.Map.of("success", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/{id}/item/{idDetalle}/nota")
    @ResponseBody
    public java.util.Map<String, Object> actualizarNotaAjax(
            @PathVariable Integer id, @PathVariable Integer idDetalle, @RequestParam String nota) {
        try {
            pedidoService.actualizarNota(idDetalle, nota);
            return java.util.Map.of("success", true, "nota", nota);
        } catch (Exception e) {
            return java.util.Map.of("success", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/{id}/item/{idDetalle}/extras")
    @ResponseBody
    public java.util.Map<String, Object> actualizarExtrasAjax(
            @PathVariable Integer id, @PathVariable Integer idDetalle,
            @RequestParam(required = false) List<Integer> idCremas,
            @RequestParam(required = false) List<Integer> idSalsas) {
        try {
            pedidoService.actualizarExtras(idDetalle, idCremas, idSalsas);
            return java.util.Map.of("success", true);
        } catch (Exception e) {
            return java.util.Map.of("success", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/{id}/cocina")
    @ResponseBody
    public java.util.Map<String, Object> enviarCocinaAjax(@PathVariable Integer id) {
        try {
            TicketCocina ticket = pedidoService.enviarACocina(id);
            if (ticket != null) {
                return java.util.Map.of(
                        "success", true,
                        "correlativo", ticket.getCorrelativo(),
                        "idTicket", ticket.getIdTicket()
                );
            } else {
                return java.util.Map.of("success", false, "error", "No hay ítems en borrador para enviar a cocina");
            }
        } catch (Exception e) {
            return java.util.Map.of("success", false, "error", e.getMessage());
        }
    }
}
