package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Gasto;
import com.cibertec.chilpayate.service.GastoService;
import com.cibertec.chilpayate.service.ProveedorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/gastos")
public class GastoController {

    private final GastoService gastoService;
    private final ProveedorService proveedorService;

    public GastoController(GastoService gastoService, ProveedorService proveedorService) {
        this.gastoService = gastoService;
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                       Model model) {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();

        model.addAttribute("gastos", gastoService.listarPorRango(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("totalGastos", gastoService.totalGastosHoy());
        return "gastos/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        Gasto g = new Gasto();
        g.setFecha(LocalDate.now());
        model.addAttribute("gasto", g);
        model.addAttribute("proveedores", proveedorService.listarActivos());
        return "gastos/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Gasto gasto, RedirectAttributes ra) {
        try {
            gastoService.guardar(gasto);
            ra.addFlashAttribute("success", "Gasto registrado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/gastos/create";
        }
        return "redirect:/gastos";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return gastoService.obtenerPorId(id).map(g -> {
            model.addAttribute("gasto", g);
            model.addAttribute("proveedores", proveedorService.listarActivos());
            return "gastos/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Gasto no encontrado");
            return "redirect:/gastos";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute Gasto gasto, RedirectAttributes ra) {
        try {
            gastoService.actualizar(id, gasto);
            ra.addFlashAttribute("success", "Gasto actualizado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/gastos/edit/" + id;
        }
        return "redirect:/gastos";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            gastoService.eliminar(id);
            ra.addFlashAttribute("success", "Gasto eliminado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el gasto");
        }
        return "redirect:/gastos";
    }
}
