package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Proveedor;
import com.cibertec.chilpayate.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "proveedores/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "proveedores/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("proveedor") Proveedor proveedor,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "proveedores/create";
        }
        try {
            proveedorService.guardar(proveedor);
            ra.addFlashAttribute("success", "Proveedor registrado exitosamente");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "proveedores/create";
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return proveedorService.obtenerPorId(id).map(p -> {
            model.addAttribute("proveedor", p);
            return "proveedores/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Proveedor no encontrado");
            return "redirect:/proveedores";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,
                       @Valid @ModelAttribute("proveedor") Proveedor proveedor,
                       BindingResult br,
                       Model model,
                       RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "proveedores/edit";
        }
        try {
            proveedorService.actualizar(id, proveedor);
            ra.addFlashAttribute("success", "Proveedor actualizado exitosamente");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "proveedores/edit";
        }
        return "redirect:/proveedores";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        proveedorService.cambiarEstado(id);
        ra.addFlashAttribute("success", "Estado del proveedor actualizado");
        return "redirect:/proveedores";
    }
}
