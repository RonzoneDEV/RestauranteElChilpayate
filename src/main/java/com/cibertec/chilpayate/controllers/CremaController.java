package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Crema;
import com.cibertec.chilpayate.service.CremaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cremas")
public class CremaController {

    private final CremaService cremaService;

    public CremaController(CremaService cremaService) {
        this.cremaService = cremaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("cremas", cremaService.listarTodos());
        return "cremas/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("crema", new Crema());
        return "cremas/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Crema crema, RedirectAttributes ra) {
        try {
            cremaService.guardar(crema);
            ra.addFlashAttribute("success", "Crema creada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/cremas/create";
        }
        return "redirect:/cremas";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return cremaService.obtenerPorId(id).map(c -> {
            model.addAttribute("crema", c);
            return "cremas/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Crema no encontrada");
            return "redirect:/cremas";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, @ModelAttribute Crema crema, RedirectAttributes ra) {
        try {
            cremaService.actualizar(id, crema);
            ra.addFlashAttribute("success", "Crema actualizada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/cremas/edit/" + id;
        }
        return "redirect:/cremas";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        cremaService.cambiarEstado(id);
        ra.addFlashAttribute("success", "Estado de la crema actualizado");
        return "redirect:/cremas";
    }
}
