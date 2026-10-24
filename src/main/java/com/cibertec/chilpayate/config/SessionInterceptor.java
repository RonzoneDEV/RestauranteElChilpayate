package com.cibertec.chilpayate.config;

import com.cibertec.chilpayate.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    private static final String[] SOLO_ADMIN = {
        "/productos", "/categorias", "/cremas", "/clientes", "/proveedores",
        "/mesas", "/usuarios", "/roles", "/stock", "/incidentes", "/reportes"
    };

    private boolean esSoloAdmin(String uri) {
        if (uri.equals("/stock/rapido") || uri.startsWith("/stock/rapido/") || uri.startsWith("/stock/api/")) {
            return false;
        }
        for (String p : SOLO_ADMIN) {
            if (uri.equals(p) || uri.startsWith(p + "/")) {
                return true;
            }
        }
        return false;
    }

    private boolean esRutaCajaOGasto(String uri) {
        return uri.equals("/caja") || uri.startsWith("/caja/") || uri.equals("/gastos") || uri.startsWith("/gastos/");
    }

    private boolean esRutaCocina(String uri) {
        return uri.equals("/cocina") || uri.startsWith("/cocina/");
    }

    private boolean esRutaStockRapido(String uri) {
        return uri.equals("/stock/rapido") || uri.startsWith("/stock/rapido/") || uri.startsWith("/stock/api/");
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();
        if (uri.equals("/") || uri.startsWith("/login") || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/img/") || uri.startsWith("/error") || uri.equals("/favicon.ico")
                || uri.startsWith("/actuator")) {
            return true;
        }

        Object o = req.getSession().getAttribute("usuario");
        if (o == null) {
            String requestedWith = req.getHeader("X-Requested-With");
            String accept = req.getHeader("Accept");
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json")) || uri.contains("/api/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"success\":false,\"error\":\"Sesión expirada. Por favor inicie sesión nuevamente.\"}");
                return false;
            }
            res.sendRedirect("/login");
            return false;
        }

        if (o instanceof Usuario u && Boolean.FALSE.equals(u.getEstado())) {
            req.getSession().invalidate();
            res.sendRedirect("/login?inactivo=true");
            return false;
        }

        String rol = null;
        if (o instanceof Usuario u && u.getRol() != null) {
            rol = u.getRol().getNombre();
        }
        String rolNorm = rol != null ? rol.trim().toUpperCase() : "";

        if (rolNorm.contains("ADMIN") || rolNorm.equals("ADMINISTRADOR")) {
            return true;
        }

        if (esSoloAdmin(uri)) {
            res.sendRedirect(rolNorm.contains("COCIN") ? "/cocina" : "/salon");
            return false;
        }

        if (rolNorm.contains("COCIN")) {
            if (!esRutaCocina(uri) && !esRutaStockRapido(uri) && !uri.equals("/logout")) {
                res.sendRedirect("/cocina");
                return false;
            }
        }

        if (rolNorm.contains("MOZO")) {
            if (esRutaCajaOGasto(uri)) {
                res.sendRedirect("/salon");
                return false;
            }
        }

        return true;
    }
}
