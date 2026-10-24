package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Rol;
import com.cibertec.chilpayate.service.RolService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", rolService.listarTodos());
        model.addAttribute("nuevoRol", new Rol());
        return "roles/list";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Rol rol, RedirectAttributes ra) {
        try {
            rolService.guardar(rol);
            ra.addFlashAttribute("success", "Nuevo rol registrado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            rolService.cambiarEstado(id);
            ra.addFlashAttribute("success", "Estado del rol actualizado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }
}
