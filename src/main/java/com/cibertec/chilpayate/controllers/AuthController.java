package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping({"/", "/login"})
    public String login() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String validar(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "correo", required = false) String correo,
            @RequestParam(value = "clave", required = false) String clave,
            HttpSession session,
            Model model) {

        String identificador = (username != null && !username.isBlank()) ? username : correo;

        log.info("Intentando login con identificador: [{}]", identificador);

        if (identificador == null || identificador.isBlank() || clave == null || clave.isBlank()) {
            model.addAttribute("mensaje", "Debe ingresar usuario y contraseña");
            return "auth/login";
        }

        Usuario usuario = usuarioService.validarAcceso(identificador, clave);

        if (usuario == null) {
            model.addAttribute("mensaje", "Usuario o contraseña incorrectos");
            return "auth/login";
        }

        session.setAttribute("usuario", usuario);
        String rol = (usuario.getRol() != null && usuario.getRol().getNombre() != null)
                ? usuario.getRol().getNombre().toUpperCase() : "";

        if (rol.contains("COCIN")) {
            return "redirect:/cocina";
        } else if (rol.contains("MOZO")) {
            return "redirect:/salon";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}