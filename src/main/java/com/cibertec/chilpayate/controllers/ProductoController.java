package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Producto;
import com.cibertec.chilpayate.service.CategoriaService;
import com.cibertec.chilpayate.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "productos/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaService.listarActivos());
        return "productos/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("producto") Producto producto,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("categorias", categoriaService.listarActivos());
            return "productos/create";
        }
        try {
            productoService.guardar(producto);
            ra.addFlashAttribute("success", "Producto creado exitosamente");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categorias", categoriaService.listarActivos());
            return "productos/create";
        }
        return "redirect:/productos";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return productoService.obtenerPorId(id).map(p -> {
            model.addAttribute("producto", p);
            model.addAttribute("categorias", categoriaService.listarActivos());
            return "productos/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/productos";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,
                       @Valid @ModelAttribute("producto") Producto producto,
                       BindingResult br,
                       Model model,
                       RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("categorias", categoriaService.listarActivos());
            return "productos/edit";
        }
        try {
            productoService.actualizar(id, producto);
            ra.addFlashAttribute("success", "Producto actualizado exitosamente");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categorias", categoriaService.listarActivos());
            return "productos/edit";
        }
        return "redirect:/productos";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        productoService.cambiarEstado(id);
        ra.addFlashAttribute("success", "Estado del producto actualizado");
        return "redirect:/productos";
    }
}
