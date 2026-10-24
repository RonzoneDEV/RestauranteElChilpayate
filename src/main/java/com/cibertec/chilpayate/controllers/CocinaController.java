package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.TicketCocina;
import com.cibertec.chilpayate.service.CocinaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cocina")
public class CocinaController {

    private final CocinaService cocinaService;

    public CocinaController(CocinaService cocinaService) {
        this.cocinaService = cocinaService;
    }

    @GetMapping
    public String monitor(Model model) {
        List<TicketCocina> ticketsActivos = cocinaService.listarTicketsActivos();
        model.addAttribute("tickets", ticketsActivos);
        return "cocina/monitor";
    }

    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, Object> statusCocina() {
        List<TicketCocina> ticketsActivos = cocinaService.listarTicketsActivos();
        List<Integer> ids = ticketsActivos.stream().map(TicketCocina::getIdTicket).toList();
        return Map.of(
                "success", true,
                "count", ticketsActivos.size(),
                "ticketIds", ids,
                "timestamp", System.currentTimeMillis()
        );
    }

    @PostMapping("/ticket/{id}/preparando")
    public String marcarPreparando(@PathVariable Integer id, RedirectAttributes ra) {
        cocinaService.marcarPreparando(id).ifPresent(t ->
                ra.addFlashAttribute("success", "Ticket #" + t.getCorrelativo() + " en preparación")
        );
        return "redirect:/cocina";
    }

    @PostMapping("/api/ticket/{id}/preparando")
    @ResponseBody
    public Map<String, Object> marcarPreparandoAjax(@PathVariable Integer id) {
        try {
            return cocinaService.marcarPreparando(id)
                    .map(t -> Map.<String, Object>of("success", true, "idTicket", id, "estado", t.getEstado().name()))
                    .orElseGet(() -> Map.of("success", false, "error", "Ticket no encontrado: " + id));
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @PostMapping({"/ticket/{id}/listo", "/ticket/{id}/completar"})
    public String marcarListo(@PathVariable Integer id, RedirectAttributes ra) {
        cocinaService.marcarListo(id).ifPresent(t ->
                ra.addFlashAttribute("success", "Ticket #" + t.getCorrelativo() + " completado y listo para despacho.")
        );
        return "redirect:/cocina";
    }

    @PostMapping("/item/{idDetalle}/toggle")
    public String toggleItemListo(@PathVariable Integer idDetalle, RedirectAttributes ra) {
        try {
            cocinaService.toggleItemListo(idDetalle);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cocina";
    }

    @PostMapping("/api/item/{idDetalle}/toggle")
    @ResponseBody
    public Map<String, Object> toggleItemListoAjax(@PathVariable Integer idDetalle) {
        try {
            return cocinaService.toggleItemListoAjax(idDetalle);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
