package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Cliente;
import com.cibertec.chilpayate.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("clientes", clienteService.buscar(q));
        model.addAttribute("q", q);
        return "clientes/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("cliente") Cliente cliente,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "clientes/create";
        }
        try {
            clienteService.guardar(cliente);
            ra.addFlashAttribute("success", "Cliente registrado exitosamente");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "clientes/create";
        }
        return "redirect:/clientes";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return clienteService.obtenerPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return "clientes/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Cliente no encontrado");
            return "redirect:/clientes";
        });
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,
                       @Valid @ModelAttribute("cliente") Cliente cliente,
                       BindingResult br,
                       Model model,
                       RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "clientes/edit";
        }
        try {
            clienteService.actualizar(id, cliente);
            ra.addFlashAttribute("success", "Cliente actualizado exitosamente");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "clientes/edit";
        }
        return "redirect:/clientes";
    }

    @GetMapping("/delete/{id}")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        clienteService.cambiarEstado(id);
        ra.addFlashAttribute("success", "Estado del cliente actualizado");
        return "redirect:/clientes";
    }
}
