package com.cibertec.chilpayate.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean esPeticionAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))
                || uri.contains("/api/");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        if (esPeticionAjax(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Parámetro inválido"
            ));
        }

        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("codigoEstado", 400);
        mav.addObject("errorTitulo", "Solicitud No Válida");
        mav.addObject("mensaje", ex.getMessage() != null ? ex.getMessage() : "Los datos proporcionados no cumplen con el formato requerido.");
        mav.addObject("ruta", request.getRequestURI());
        mav.addObject("fechaHora", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return mav;
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        if (esPeticionAjax(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Conflicto de estado"
            ));
        }

        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("codigoEstado", 409);
        mav.addObject("errorTitulo", "Operación no permitida en el estado actual");
        mav.addObject("mensaje", ex.getMessage() != null ? ex.getMessage() : "El recurso solicitado no se encuentra en el estado adecuado para esta acción.");
        mav.addObject("ruta", request.getRequestURI());
        mav.addObject("fechaHora", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return mav;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado al procesar solicitud URI: {}", request.getRequestURI(), ex);

        if (esPeticionAjax(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Ocurrió un error inesperado al procesar la solicitud: " + ex.getMessage()
            ));
        }

        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("codigoEstado", 500);
        mav.addObject("errorTitulo", "Incidencia Interna del Servidor");
        mav.addObject("mensaje", "Ocurrió un contratiempo inesperado. El equipo de soporte ha sido notificado para solucionarlo.");
        mav.addObject("detalleTecnico", ex.getMessage());
        mav.addObject("ruta", request.getRequestURI());
        mav.addObject("fechaHora", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return mav;
    }
}
