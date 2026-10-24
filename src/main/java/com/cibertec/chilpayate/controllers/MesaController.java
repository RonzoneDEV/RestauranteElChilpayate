package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Mesa;
import com.cibertec.chilpayate.service.MesaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("mesas", mesaService.listarTodas());
        return "mesas/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("mesa", new Mesa());
        return "mesas/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("mesa") Mesa mesa,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "mesas/create";
        }
        try {
            mesaService.guardar(mesa);
            ra.addFlashAttribute("success", "Mesa registrada exitosamente");
        } catch (Exception e) {
            br.rejectValue("numero", "error.numero", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "mesas/create";
        }
        return "redirect:/mesas";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return mesaService.obtenerPorId(id).map(m -> {
            if (m.getEstado() == Mesa.EstadoMesa.OCUPADA) {
                ra.addFlashAttribute("error", "No se puede editar la Mesa #" + m.getNumero() + " (" + m.getSalon() + ") porque se encuentra actualmente OCUPADA en atención.");
                return "redirect:/mesas";
            }
            model.addAttribute("mesa", m);
            return "mesas/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Mesa no encontrada");
            return "redirect:/mesas";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,
                       @Valid @ModelAttribute("mesa") Mesa mesa,
                       BindingResult br,
                       Model model,
                       RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "mesas/edit";
        }
        try {
            mesaService.actualizar(id, mesa);
            ra.addFlashAttribute("success", "Mesa actualizada exitosamente");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/mesas";
        } catch (Exception e) {
            br.rejectValue("numero", "error.numero", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "mesas/edit";
        }
        return "redirect:/mesas";
    }

    @GetMapping("/delete/{id}")
    public String toggleActivo(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            mesaService.cambiarActivo(id);
            ra.addFlashAttribute("success", "Estado de activación de la mesa actualizado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mesas";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstadoMesa(@PathVariable Integer id, @RequestParam Mesa.EstadoMesa estado, RedirectAttributes ra) {
        mesaService.cambiarEstado(id, estado);
        ra.addFlashAttribute("success", "Estado de la mesa cambiado a " + estado);
        return "redirect:/mesas";
    }
}
