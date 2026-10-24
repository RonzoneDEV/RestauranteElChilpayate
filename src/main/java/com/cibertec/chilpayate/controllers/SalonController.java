package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Mesa;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.service.CarritoService;
import com.cibertec.chilpayate.service.MesaService;
import com.cibertec.chilpayate.service.PedidoService;
import com.cibertec.chilpayate.service.PrecuentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/salon")
public class SalonController {

    private final MesaService mesaService;
    private final PedidoService pedidoService;
    private final PrecuentaService precuentaService;
    private final CarritoService carritoService;

    public SalonController(MesaService mesaService,
                           PedidoService pedidoService,
                           PrecuentaService precuentaService,
                           CarritoService carritoService) {
        this.mesaService = mesaService;
        this.pedidoService = pedidoService;
        this.precuentaService = precuentaService;
        this.carritoService = carritoService;
    }

    @GetMapping
    public String index(Model model, HttpSession session) {
        List<Mesa> mesas = mesaService.listarActivas();
        Map<Integer, Pedido> pedidosPorMesa = new HashMap<>();

        for (Mesa m : mesas) {
            Optional<Pedido> pOpt = pedidoService.obtenerPorMesaActiva(m.getIdMesa());
            if (pOpt.isPresent()) {
                pedidosPorMesa.put(m.getIdMesa(), pOpt.get());
                if (m.getEstado() == Mesa.EstadoMesa.LIBRE) {
                    m.setEstado(Mesa.EstadoMesa.OCUPADA);
                    mesaService.guardar(m);
                }
            }
        }

        long totalMesas = mesas.size();
        long ocupadas = mesas.stream().filter(m -> m.getEstado() == Mesa.EstadoMesa.OCUPADA).count();
        long bloqueadas = mesas.stream().filter(m -> m.getEstado() == Mesa.EstadoMesa.BLOQUEADA).count();
        long libres = mesas.stream().filter(m -> m.getEstado() == Mesa.EstadoMesa.LIBRE).count();

        model.addAttribute("mesas", mesas);
        model.addAttribute("pedidosPorMesa", pedidosPorMesa);
        model.addAttribute("carritosPorMesa", carritoService.listarCarritos(session));
        model.addAttribute("totalMesas", totalMesas);
        model.addAttribute("ocupadas", ocupadas);
        model.addAttribute("bloqueadas", bloqueadas);
        model.addAttribute("libres", libres);

        return "salon/index";
    }

    @GetMapping("/mesa/{idMesa}")
    public String abrirMesa(@PathVariable Integer idMesa, HttpSession session, RedirectAttributes ra) {
        try {
            Optional<Mesa> mesaOpt = mesaService.obtenerPorId(idMesa);
            if (mesaOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Mesa no encontrada");
                return "redirect:/salon";
            }
            Mesa mesa = mesaOpt.get();
            if (mesa.getEstado() == Mesa.EstadoMesa.BLOQUEADA) {
                ra.addFlashAttribute("error", "⛔ Acceso bloqueado: La Mesa #" + mesa.getNumero() + " está BLOQUEADA por precuenta emitida en cobro. No se permite agregar más productos hasta cancelar todas las precuentas.");
                return "redirect:/salon";
            }
            return "redirect:/pedidos/mesa/" + idMesa;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al abrir la mesa: " + e.getMessage());
            return "redirect:/salon";
        }
    }

    @GetMapping("/liberar/{idMesa}")
    public String liberarMesa(@PathVariable Integer idMesa, HttpSession session, RedirectAttributes ra) {
        try {
            Optional<Mesa> mesaOpt = mesaService.obtenerPorId(idMesa);
            if (mesaOpt.isPresent() && mesaOpt.get().getEstado() == Mesa.EstadoMesa.BLOQUEADA) {
                ra.addFlashAttribute("error", "No se puede liberar la Mesa #" + mesaOpt.get().getNumero() + " porque está BLOQUEADA con precuenta en cobro. Debe cancelarse en Caja.");
                return "redirect:/salon";
            }

            carritoService.limpiarCarrito(session, idMesa);
            Optional<Pedido> pedidoOpt = pedidoService.obtenerPorMesaActiva(idMesa);
            if (pedidoOpt.isPresent()) {
                precuentaService.eliminarPrecuenta(pedidoOpt.get().getIdPedido());
                pedidoService.anularPedido(pedidoOpt.get().getIdPedido(), "Mesa liberada desde Salón");
            } else {
                mesaService.cambiarEstado(idMesa, Mesa.EstadoMesa.LIBRE);
            }
            ra.addFlashAttribute("success", "Mesa liberada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al liberar mesa: " + e.getMessage());
        }
        return "redirect:/salon";
    }
}
