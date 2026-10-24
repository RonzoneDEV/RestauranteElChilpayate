package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Mesa;
import com.cibertec.chilpayate.model.Producto;
import com.cibertec.chilpayate.model.cart.CarritoDelivery;
import com.cibertec.chilpayate.model.cart.CarritoMesa;
import com.cibertec.chilpayate.model.cart.ItemCarrito;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarritoService {

    public static final String SESSION_CARROS_SALON = "CARROS_SALON_SESSION";
    public static final String SESSION_CARRITO_DELIVERY = "CARRITO_DELIVERY_SESSION";

    @SuppressWarnings("unchecked")
    private Map<Integer, CarritoMesa> obtenerMapaCarritos(HttpSession session) {
        Map<Integer, CarritoMesa> map = (Map<Integer, CarritoMesa>) session.getAttribute(SESSION_CARROS_SALON);
        if (map == null) {
            map = new HashMap<>();
            session.setAttribute(SESSION_CARROS_SALON, map);
        }
        return map;
    }

    public CarritoMesa obtenerCarrito(HttpSession session, Mesa mesa) {
        if (mesa == null || mesa.getIdMesa() == null) return new CarritoMesa();
        Map<Integer, CarritoMesa> map = obtenerMapaCarritos(session);
        return map.computeIfAbsent(mesa.getIdMesa(), k -> new CarritoMesa(mesa.getIdMesa(), mesa.getNumero(), mesa.getSalon()));
    }

    public void agregarItem(HttpSession session, Mesa mesa, Producto producto,
                            List<Integer> idSalsas, List<String> nombresSalsas,
                            String nota) {
        CarritoMesa carrito = obtenerCarrito(session, mesa);
        String categoriaNombre = (producto.getCategoria() != null) ? producto.getCategoria().getNombre() : "Plato";
        ItemCarrito item = new ItemCarrito(
                producto.getIdProducto(),
                producto.getNombre(),
                categoriaNombre,
                producto.getTipo(),
                producto.getPrecio(),
                nota,
                idSalsas,
                nombresSalsas
        );
        carrito.agregarItem(item);
    }

    public int contarPorProducto(HttpSession session, Integer idMesa, Integer idProducto) {
        Map<Integer, CarritoMesa> map = obtenerMapaCarritos(session);
        CarritoMesa c = map.get(idMesa);
        return c != null ? c.contarPorProducto(idProducto) : 0;
    }

    public void eliminar(HttpSession session, Integer idMesa, String idItem) {
        Map<Integer, CarritoMesa> map = obtenerMapaCarritos(session);
        CarritoMesa c = map.get(idMesa);
        if (c != null) {
            c.eliminarItem(idItem);
        }
    }

    public void actualizarNota(HttpSession session, Integer idMesa, String idItem, String nota) {
        Map<Integer, CarritoMesa> map = obtenerMapaCarritos(session);
        CarritoMesa c = map.get(idMesa);
        if (c != null) {
            c.actualizarNota(idItem, nota);
        }
    }

    public void limpiarCarrito(HttpSession session, Integer idMesa) {
        Map<Integer, CarritoMesa> map = obtenerMapaCarritos(session);
        map.remove(idMesa);
    }

    public Map<Integer, CarritoMesa> listarCarritos(HttpSession session) {
        return obtenerMapaCarritos(session);
    }

    // ==========================================
    // MÉTODOS PARA CARRITO DE DELIVERY
    // ==========================================

    public CarritoDelivery obtenerCarritoDelivery(HttpSession session) {
        CarritoDelivery carrito = (CarritoDelivery) session.getAttribute(SESSION_CARRITO_DELIVERY);
        if (carrito == null) {
            carrito = new CarritoDelivery();
            session.setAttribute(SESSION_CARRITO_DELIVERY, carrito);
        }
        return carrito;
    }

    public void agregarItemDelivery(HttpSession session, Producto producto,
                                    List<Integer> idCremas, List<String> nombresCremas,
                                    List<Integer> idSalsas, List<String> nombresSalsas,
                                    String nota) {
        CarritoDelivery carrito = obtenerCarritoDelivery(session);
        String categoriaNombre = (producto.getCategoria() != null) ? producto.getCategoria().getNombre() : "Plato";
        ItemCarrito item = new ItemCarrito(
                producto.getIdProducto(),
                producto.getNombre(),
                categoriaNombre,
                producto.getTipo(),
                producto.getPrecio(),
                nota,
                idCremas,
                nombresCremas,
                idSalsas,
                nombresSalsas
        );
        carrito.agregarItem(item);
    }

    public int contarPorProductoDelivery(HttpSession session, Integer idProducto) {
        CarritoDelivery carrito = (CarritoDelivery) session.getAttribute(SESSION_CARRITO_DELIVERY);
        return carrito != null ? carrito.contarPorProducto(idProducto) : 0;
    }

    public void eliminarItemDelivery(HttpSession session, String idItem) {
        CarritoDelivery carrito = (CarritoDelivery) session.getAttribute(SESSION_CARRITO_DELIVERY);
        if (carrito != null) {
            carrito.eliminarItem(idItem);
        }
    }

    public void actualizarNotaDelivery(HttpSession session, String idItem, String nota) {
        CarritoDelivery carrito = (CarritoDelivery) session.getAttribute(SESSION_CARRITO_DELIVERY);
        if (carrito != null) {
            carrito.actualizarNota(idItem, nota);
        }
    }

    public void actualizarDatosDespachoDelivery(HttpSession session, String nombreCliente, String telefono,
                                               String direccion, String referencia,
                                               BigDecimal costoDelivery, String repartidor) {
        CarritoDelivery carrito = obtenerCarritoDelivery(session);
        if (nombreCliente != null) carrito.setNombreCliente(nombreCliente.trim());
        if (telefono != null) carrito.setTelefono(telefono.trim());
        if (direccion != null) carrito.setDireccion(direccion.trim());
        if (referencia != null) carrito.setReferencia(referencia.trim());
        if (costoDelivery != null) carrito.setCostoDelivery(costoDelivery);
        if (repartidor != null) carrito.setRepartidor(repartidor.trim());
    }

    public void limpiarCarritoDelivery(HttpSession session) {
        session.removeAttribute(SESSION_CARRITO_DELIVERY);
    }
}
