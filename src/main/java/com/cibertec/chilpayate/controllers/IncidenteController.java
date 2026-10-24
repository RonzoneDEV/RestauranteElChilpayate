package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Incidente;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.service.IncidenteService;
import com.cibertec.chilpayate.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/incidentes")
public class IncidenteController {

    private final IncidenteService incidenteService;
    private final PedidoService pedidoService;

    public IncidenteController(IncidenteService incidenteService, PedidoService pedidoService) {
        this.incidenteService = incidenteService;
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String tipo, Model model) {
        List<Incidente> incidentes = incidenteService.listar(tipo);

        long totalIncidentes = incidentes.size();
        long conDevolucionStock = incidentes.stream().filter(i -> Boolean.TRUE.equals(i.getDevolvioStock())).count();

        model.addAttribute("incidentes", incidentes);
        model.addAttribute("pedidos", pedidoService.listarTodos());
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("totalIncidentes", totalIncidentes);
        model.addAttribute("conDevolucionStock", conDevolucionStock);
        return "incidentes/list";
    }

    @PostMapping("/create")
    public String create(@RequestParam(required = false) Integer idPedido,
                         @RequestParam String tipo,
                         @RequestParam String detalle,
                         @RequestParam String motivo,
                         @RequestParam(required = false, defaultValue = "false") Boolean devolvioStock,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            Usuario u = (Usuario) session.getAttribute("usuario");
            Pedido pedido = idPedido != null ? pedidoService.obtenerPorId(idPedido).orElse(null) : null;

            incidenteService.registrar(pedido, tipo, detalle, motivo, devolvioStock, u);
            ra.addFlashAttribute("success", "Incidente registrado y reportado exitosamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/incidentes";
    }
}
