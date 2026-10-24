package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Categoria;
import com.cibertec.chilpayate.model.Producto;
import com.cibertec.chilpayate.model.TipoProducto;
import com.cibertec.chilpayate.model.cart.CarritoDelivery;
import com.cibertec.chilpayate.model.cart.CarritoMesa;
import com.cibertec.chilpayate.model.cart.ItemCarrito;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ParametrosCartaAndCarritoTest {

    @Test
    @DisplayName("Salsas se infieren como EXTRA y adicionales comunes como ADICIONAL")
    void testInferirTipoExtraVsAdicional() {
        Categoria catSalsa = new Categoria();
        catSalsa.setNombre("Salsas");

        Producto salsa = new Producto();
        salsa.setNombre("BBQ");
        salsa.setCategoria(catSalsa);
        assertEquals(TipoProducto.EXTRA, salsa.inferirTipo(), "Salsa debe inferir tipo EXTRA para evitar conflicto con adicionales");
        assertTrue(salsa.esExtra());
        assertFalse(salsa.esAdicional());

        Categoria catAdic = new Categoria();
        catAdic.setNombre("Adicionales");

        Producto adic = new Producto();
        adic.setNombre("Huevo Montado");
        adic.setCategoria(catAdic);
        assertEquals(TipoProducto.ADICIONAL, adic.inferirTipo(), "Adicionales comunes deben inferir ADICIONAL");
        assertTrue(adic.esAdicional());
        assertFalse(adic.esExtra());

        Categoria catAlitas = new Categoria();
        catAlitas.setNombre("Alitas");

        Producto alitas = new Producto();
        alitas.setNombre("Alitas Combo x 12");
        alitas.setCategoria(catAlitas);
        assertEquals(TipoProducto.PRINCIPAL, alitas.inferirTipo());
        assertTrue(alitas.esPrincipal());
        assertTrue(alitas.esAlitas());
    }

    @Test
    @DisplayName("Cálculo proporcional de exceso de salsas en CarritoMesa al eliminar porciones de alitas")
    void testExcesoSalsasProporcionalEnCarritoMesa() {
        CarritoMesa carrito = new CarritoMesa(1, 1, "Salón Principal");

        // Agregar 2 porciones de alitas
        ItemCarrito alita1 = new ItemCarrito(10, "Alitas Combo x6", "Alitas", TipoProducto.PRINCIPAL, new BigDecimal("18.00"), "", Collections.emptyList(), Collections.emptyList());
        ItemCarrito alita2 = new ItemCarrito(11, "Alitas Combo x12", "Alitas", TipoProducto.PRINCIPAL, new BigDecimal("34.00"), "", Collections.emptyList(), Collections.emptyList());
        carrito.agregarItem(alita1);
        carrito.agregarItem(alita2);

        // Con 2 porciones y parámetro de 2 salsas por alitas => max permitido = 2 * 2 = 4 salsas
        int maxPorAlita = 2;
        assertEquals(2, carrito.contarAlitas());
        assertEquals(0, carrito.calcularExcesoSalsas(maxPorAlita));

        // Agregar 4 salsas extras
        for (int i = 0; i < 4; i++) {
            ItemCarrito salsa = new ItemCarrito(20 + i, "Salsa BBQ " + i, "Salsas", TipoProducto.EXTRA, new BigDecimal("3.00"), "", Collections.emptyList(), Collections.emptyList());
            carrito.agregarItem(salsa);
        }

        assertEquals(4, carrito.contarExtrasSalsas());
        assertEquals(0, carrito.calcularExcesoSalsas(maxPorAlita), "Con 2 alitas y 4 salsas no debe haber exceso");
        assertFalse(carrito.tieneExcesos(maxPorAlita, 2));

        // El usuario elimina 1 porción de alitas del carrito
        carrito.eliminarItem(alita1.getIdItem());

        // Ahora queda 1 porción de alitas y 4 salsas => permitido = 2, exceso = 4 - 2 = 2
        assertEquals(1, carrito.contarAlitas());
        assertEquals(4, carrito.contarExtrasSalsas());
        assertEquals(2, carrito.calcularExcesoSalsas(maxPorAlita), "Al eliminar una porción de alitas debe calcular 2 salsas en exceso");
        assertTrue(carrito.tieneExcesos(maxPorAlita, 2), "El pedido debe marcar exceso y bloquear envío a cocina");

        // El usuario elimina la última porción de alitas
        carrito.eliminarItem(alita2.getIdItem());

        // Ahora quedan 0 alitas y 4 salsas => permitido = 0, exceso = 4
        assertEquals(0, carrito.contarAlitas());
        assertEquals(4, carrito.calcularExcesoSalsas(maxPorAlita), "Con 0 alitas todas las 4 salsas deben calcularse como exceso");
        assertTrue(carrito.tieneExcesos(maxPorAlita, 2));
    }

    @Test
    @DisplayName("Cálculo proporcional de exceso de adicionales en CarritoDelivery")
    void testExcesoAdicionalesProporcionalEnCarritoDelivery() {
        CarritoDelivery carrito = new CarritoDelivery();

        // Agregar 1 plato principal (Hamburguesa)
        ItemCarrito burger = new ItemCarrito(100, "Hamburguesa Clásica", "Hamburguesas", TipoProducto.PRINCIPAL, new BigDecimal("16.00"), "", Collections.emptyList(), Collections.emptyList());
        carrito.agregarItem(burger);

        int maxPorPrincipal = 2;
        assertEquals(1, carrito.contarPlatosPrincipales());

        // Agregar 2 adicionales
        ItemCarrito adic1 = new ItemCarrito(200, "Porción de Queso", "Adicionales", TipoProducto.ADICIONAL, new BigDecimal("3.00"), "", Collections.emptyList(), Collections.emptyList());
        ItemCarrito adic2 = new ItemCarrito(201, "Huevo Frito", "Adicionales", TipoProducto.ADICIONAL, new BigDecimal("2.50"), "", Collections.emptyList(), Collections.emptyList());
        carrito.agregarItem(adic1);
        carrito.agregarItem(adic2);

        assertEquals(2, carrito.contarAdicionales());
        assertEquals(0, carrito.calcularExcesoAdicionales(maxPorPrincipal));
        assertFalse(carrito.tieneExcesos(2, maxPorPrincipal));

        // Agregar un 3er adicional => exceso de 1
        ItemCarrito adic3 = new ItemCarrito(202, "Tocinillo Extra", "Adicionales", TipoProducto.ADICIONAL, new BigDecimal("4.00"), "", Collections.emptyList(), Collections.emptyList());
        carrito.agregarItem(adic3);

        assertEquals(3, carrito.contarAdicionales());
        assertEquals(1, carrito.calcularExcesoAdicionales(maxPorPrincipal), "3 adicionales para 1 plato principal genera 1 adicional en exceso");
        assertTrue(carrito.tieneExcesos(2, maxPorPrincipal));

        // Si eliminamos la hamburguesa, quedan 0 principales y 3 adicionales => exceso = 3
        carrito.eliminarItem(burger.getIdItem());
        assertEquals(0, carrito.contarPlatosPrincipales());
        assertEquals(3, carrito.calcularExcesoAdicionales(maxPorPrincipal));
        assertTrue(carrito.tieneExcesos(2, maxPorPrincipal));
    }
}
