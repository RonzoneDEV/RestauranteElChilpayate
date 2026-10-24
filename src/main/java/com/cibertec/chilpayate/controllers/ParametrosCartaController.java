package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.ParametrosCarta;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.service.CategoriaService;
import com.cibertec.chilpayate.service.ParametrosCartaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/parametros-carta", "/categorias/parametros"})
public class ParametrosCartaController {

    private final ParametrosCartaService parametrosService;
    private final CategoriaService categoriaService;

    public ParametrosCartaController(ParametrosCartaService parametrosService, CategoriaService categoriaService) {
        this.parametrosService = parametrosService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String verParametros(Model model) {
        ParametrosCarta params = parametrosService.obtenerParametros();
        model.addAttribute("parametros", params);
        model.addAttribute("categorias", categoriaService.listarTodos());
        return "parametros/carta";
    }

    @PostMapping
    public String guardarParametros(@ModelAttribute ParametrosCarta form,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            String username = usuario != null ? usuario.getUsername() : "admin";

            ParametrosCarta guardado = parametrosService.guardarParametros(form, username);
            ra.addFlashAttribute("success", "✓ Parámetros de la Carta actualizados correctamente. " +
                    "Límite de salsas por alitas: " + guardado.getMaxExtrasSalsasPorAlita() + ", " +
                    "límite de adicionales por plato principal: " + guardado.getMaxAdicionalesPorPlatoPrincipal() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar parámetros: " + e.getMessage());
        }
        return "redirect:/parametros-carta";
    }
}
