package com.cibertec.chilpayate.config;

import com.cibertec.chilpayate.model.Configuracion;
import com.cibertec.chilpayate.service.ConfiguracionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final ConfiguracionService configuracionService;

    public GlobalControllerAdvice(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @ModelAttribute("configuracion")
    public Configuracion populateConfiguracion() {
        return configuracionService.obtenerConfiguracion();
    }
}
