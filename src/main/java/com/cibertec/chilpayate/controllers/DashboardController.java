package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.service.CajaService;
import com.cibertec.chilpayate.service.MesaService;
import com.cibertec.chilpayate.service.PedidoService;
import com.cibertec.chilpayate.service.ReporteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final MesaService mesaService;
    private final PedidoService pedidoService;
    private final ReporteService reporteService;
    private final CajaService cajaService;

    public DashboardController(MesaService mesaService,
                               PedidoService pedidoService,
                               ReporteService reporteService,
                               CajaService cajaService) {
        this.mesaService = mesaService;
        this.pedidoService = pedidoService;
        this.reporteService = reporteService;
        this.cajaService = cajaService;
    }

    @GetMapping("/dashboard")
    public String index(Model model) {
        model.addAttribute("resumen", reporteService.resumenGeneral());
        model.addAttribute("mesas", mesaService.listarActivas());
        model.addAttribute("pedidosActivos", pedidoService.listarActivos());
        model.addAttribute("cajaAbierta", cajaService.obtenerCajaAbierta().orElse(null));
        return "dashboard/index";
    }
}
