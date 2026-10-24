package com.cibertec.chilpayate.controllers;

import com.cibertec.chilpayate.model.Categoria;
import com.cibertec.chilpayate.model.MovimientoStock;
import com.cibertec.chilpayate.model.Producto;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.service.CategoriaService;
import com.cibertec.chilpayate.service.ProductoService;
import com.cibertec.chilpayate.service.StockService;
import com.cibertec.chilpayate.service.UsuarioService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StockController(StockService stockService,
                           ProductoService productoService,
                           CategoriaService categoriaService,
                           UsuarioService usuarioService) {
        this.stockService = stockService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String kardex(@RequestParam(required = false) Integer idProducto, Model model) {
        List<MovimientoStock> movimientos = stockService.listarMovimientos(idProducto);
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("idProductoSeleccionado", idProducto);
        return "stock/list";
    }

    /**
     * Vista de Control Rápido de Stock (Express) para el servicio del día y cocina/bar.
     */
    @GetMapping("/rapido")
    public String stockRapido(Model model) throws JsonProcessingException {
        List<Producto> productos = productoService.listarActivos();
        List<Categoria> categorias = categoriaService.listarActivos();

        List<Map<String, Object>> prodsData = new ArrayList<>();
        for (Producto p : productos) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getIdProducto());
            m.put("nombre", p.getNombre());
            m.put("codigo", "P" + String.format("%03d", p.getIdProducto()));
            m.put("idCategoria", p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null);
            m.put("categoria", p.getCategoria() != null ? p.getCategoria().getNombre() : "General");
            m.put("stock", p.getStock() != null ? p.getStock() : 0);
            m.put("precio", p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO);
            m.put("minCritico", 5);
            m.put("controlaStock", Boolean.TRUE.equals(p.getControlaStock()));
            m.put("imagen", p.getImagen());
            prodsData.add(m);
        }

        List<Map<String, Object>> catsData = new ArrayList<>();
        Map<String, Object> todos = new HashMap<>();
        todos.put("id", "TODOS");
        todos.put("label", "Todos");
        todos.put("icon", "ph-squares-four");
        catsData.add(todos);

        for (Categoria c : categorias) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getIdCategoria().toString());
            m.put("idCategoria", c.getIdCategoria());
            m.put("label", c.getNombre());
            m.put("icon", resolverIconoCategoria(c.getNombre()));
            catsData.add(m);
        }

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("productosJson", objectMapper.writeValueAsString(prodsData));
        model.addAttribute("categoriasJson", objectMapper.writeValueAsString(catsData));
        return "stock/rapido";
    }

    @PostMapping("/api/ajustar")
    @ResponseBody
    public Map<String, Object> ajustarStockRapido(@RequestParam Integer idProducto,
                                                  @RequestParam Integer delta,
                                                  HttpSession session) {
        try {
            Usuario u = (Usuario) session.getAttribute("usuario");
            if (u == null) {
                u = usuarioService.obtenerPorUsername("admin").orElse(null);
            }
            MovimientoStock mov = stockService.ajustarStockRapido(idProducto, delta, u);
            return Map.of(
                    "success", true,
                    "idProducto", idProducto,
                    "nombre", mov.getProducto().getNombre(),
                    "stockAnterior", mov.getStockAnterior(),
                    "stockNuevo", mov.getStockNuevo(),
                    "mensaje", mov.getProducto().getNombre() + ": " + mov.getStockAnterior() + " ➔ " + mov.getStockNuevo() + " uds"
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Error al ajustar stock");
        }
    }

    @PostMapping("/api/fijar")
    @ResponseBody
    public Map<String, Object> fijarStockDirecto(@RequestParam Integer idProducto,
                                                 @RequestParam Integer nuevoStock,
                                                 @RequestParam(required = false) String motivo,
                                                 HttpSession session) {
        try {
            Usuario u = (Usuario) session.getAttribute("usuario");
            if (u == null) {
                u = usuarioService.obtenerPorUsername("admin").orElse(null);
            }
            MovimientoStock mov = stockService.fijarStockDirecto(idProducto, nuevoStock, u, motivo);
            String msg = nuevoStock == 0
                    ? "🔴 " + mov.getProducto().getNombre() + " marcado como AGOTADO"
                    : mov.getProducto().getNombre() + ": Stock fijado en " + nuevoStock + " uds";
            return Map.of(
                    "success", true,
                    "idProducto", idProducto,
                    "nombre", mov.getProducto().getNombre(),
                    "stockAnterior", mov.getStockAnterior(),
                    "stockNuevo", mov.getStockNuevo(),
                    "mensaje", msg
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Error al fijar stock");
        }
    }

    @PostMapping("/api/apertura-turno")
    @ResponseBody
    public Map<String, Object> aperturaTurno(@RequestParam(required = false) Integer idCategoria,
                                             @RequestParam(required = false) String categoriaNombre,
                                             @RequestParam Integer cantidadBase,
                                             HttpSession session) {
        try {
            Usuario u = (Usuario) session.getAttribute("usuario");
            if (u == null) {
                u = usuarioService.obtenerPorUsername("admin").orElse(null);
            }
            int modificados = stockService.aperturaTurnoPorCategoria(idCategoria, categoriaNombre, cantidadBase, u);
            String catLabel = categoriaNombre != null && !categoriaNombre.isBlank() && !categoriaNombre.equals("TODOS")
                    ? categoriaNombre
                    : "Categoría general";
            return Map.of(
                    "success", true,
                    "modificados", modificados,
                    "cantidadBase", cantidadBase,
                    "mensaje", "✨ Se asignaron " + cantidadBase + " uds a " + modificados + " items agotados (" + catLabel + ")"
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Error en apertura de turno");
        }
    }

    @PostMapping("/registrar")
    public String registrarMovimiento(@RequestParam Integer idProducto,
                                      @RequestParam MovimientoStock.TipoMovimiento tipoMovimiento,
                                      @RequestParam Integer cantidad,
                                      @RequestParam String motivo,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        try {
            Usuario u = (Usuario) session.getAttribute("usuario");
            MovimientoStock mov = stockService.registrarMovimiento(idProducto, tipoMovimiento, cantidad, motivo, u);
            ra.addFlashAttribute("success", "Movimiento de stock registrado: " + mov.getProducto().getNombre() + " (Nuevo stock: " + mov.getStockNuevo() + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock";
    }

    private String resolverIconoCategoria(String nombre) {
        if (nombre == null) return "ph-tag";
        String n = nombre.toLowerCase();
        if (n.contains("alita") || n.contains("pollo") || n.contains("broaster")) return "ph-bowl-food";
        if (n.contains("bebida") || n.contains("trago") || n.contains("coctel") || n.contains("refresco")) return "ph-brandy";
        if (n.contains("salsa")) return "ph-drop";
        if (n.contains("salchipapa") || n.contains("papa") || n.contains("guarnicion")) return "ph-french-fries";
        if (n.contains("hamburguesa")) return "ph-hamburger";
        if (n.contains("postre")) return "ph-cake";
        return "ph-fork-knife";
    }
}
