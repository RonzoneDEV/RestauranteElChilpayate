package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Categoria;
import com.cibertec.chilpayate.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categorias", categoriaService.listarTodos());
        return "categorias/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("categoria", new Categoria());
        return "categorias/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Categoria categoria, RedirectAttributes ra) {
        try {
            categoriaService.guardar(categoria);
            ra.addFlashAttribute("success", "Categoría creada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias/create";
        }
        return "redirect:/categorias";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return categoriaService.obtenerPorId(id).map(c -> {
            model.addAttribute("categoria", c);
            return "categorias/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Categoría no encontrada");
            return "redirect:/categorias";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, @ModelAttribute Categoria categoria, RedirectAttributes ra) {
        try {
            categoriaService.actualizar(id, categoria);
            ra.addFlashAttribute("success", "Categoría actualizada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias/edit/" + id;
        }
        return "redirect:/categorias";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            categoriaService.cambiarEstado(id);
            ra.addFlashAttribute("success", "Estado de la categoría actualizado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categorias";
    }
}
