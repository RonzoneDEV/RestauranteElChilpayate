package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuarios/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", usuarioService.listarRoles());
        return "usuarios/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Usuario usuario, RedirectAttributes ra) {
        try {
            usuarioService.guardar(usuario);
            ra.addFlashAttribute("success", "Usuario creado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/create";
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return usuarioService.obtenerPorId(id).map(u -> {
            model.addAttribute("usuario", u);
            model.addAttribute("roles", usuarioService.listarRoles());
            return "usuarios/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/usuarios";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, @ModelAttribute Usuario usuario, RedirectAttributes ra) {
        try {
            usuarioService.actualizar(id, usuario);
            ra.addFlashAttribute("success", "Usuario actualizado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/edit/" + id;
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        usuarioService.cambiarEstado(id);
        ra.addFlashAttribute("success", "Estado del usuario actualizado");
        return "redirect:/usuarios";
    }
}
