package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Configuracion;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.service.ConfiguracionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    private boolean esAdministrador(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && usuario.getRol() != null &&
                usuario.getRol().getNombre() != null &&
                usuario.getRol().getNombre().toUpperCase().contains("ADMIN");
    }

    @GetMapping
    public String verConfiguracion(Model model, HttpSession session, RedirectAttributes ra) {
        if (!esAdministrador(session)) {
            ra.addFlashAttribute("error", "Acceso denegado: Solo el Administrador tiene autorización para ver y modificar los datos de la empresa.");
            return "redirect:/dashboard";
        }

        Configuracion config = configuracionService.obtenerConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/index";
    }

    @PostMapping
    public String guardarConfiguracion(@ModelAttribute("config") Configuracion form,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        if (!esAdministrador(session)) {
            ra.addFlashAttribute("error", "Acceso denegado: No tiene permisos de Administrador para modificar los datos de la empresa.");
            return "redirect:/dashboard";
        }

        try {
            // Validaciones básicas de negocio
            if (form.getNombre() == null || form.getNombre().trim().isBlank()) {
                throw new IllegalArgumentException("El nombre o razón social de la empresa es obligatorio.");
            }
            if (form.getRuc() == null || form.getRuc().trim().length() != 11 || !form.getRuc().trim().matches("\\d{11}")) {
                throw new IllegalArgumentException("El RUC debe constar exactamente de 11 dígitos numéricos.");
            }
            if (form.getDireccion() == null || form.getDireccion().trim().isBlank()) {
                throw new IllegalArgumentException("La dirección fiscal o del local es obligatoria.");
            }
            if (form.getInicioTurno() == null || form.getFinTurno() == null) {
                throw new IllegalArgumentException("Los horarios de inicio y fin de turno son obligatorios.");
            }

            Configuracion guardada = configuracionService.guardar(form);
            ra.addFlashAttribute("success", "✓ Datos de la empresa actualizados exitosamente. " +
                    "La información (" + guardada.getNombre() + " - RUC: " + guardada.getRuc() + ") se reflejará de inmediato en los comprobantes y tickets.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar configuración: " + e.getMessage());
        }

        return "redirect:/configuracion";
    }
}
