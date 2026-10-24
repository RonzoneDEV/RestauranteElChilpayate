package com.cibertec.chilpayate.model.cart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CarritoMesa implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idMesa;
    private Integer numeroMesa;
    private String salon;
    private List<ItemCarrito> items = new ArrayList<>();

    public CarritoMesa() {
    }

    public CarritoMesa(Integer idMesa, Integer numeroMesa, String salon) {
        this.idMesa = idMesa;
        this.numeroMesa = numeroMesa;
        this.salon = salon;
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    public int getCantidadTotal() {
        return items.size();
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    /**
     * Determina si el carrito ya contiene al menos un plato principal.
     */
    public boolean tienePlatoPrincipal() {
        return contarPlatosPrincipales() > 0;
    }

    /**
     * Cuenta la cantidad de porciones de alitas en el carrito.
     */
    public int contarAlitas() {
        if (items == null) return 0;
        int count = 0;
        for (ItemCarrito i : items) {
            if (i.esAlitas()) {
                count += i.getCantidad();
            }
        }
        return count;
    }

    /**
     * Cuenta la cantidad de platos principales en el carrito (incluye alitas).
     */
    public int contarPlatosPrincipales() {
        if (items == null) return 0;
        int count = 0;
        for (ItemCarrito i : items) {
            if (i.esPrincipal()) {
                count += i.getCantidad();
            }
        }
        return count;
    }

    /**
     * Cuenta la cantidad de extras en salsas en el carrito.
     */
    public int contarExtrasSalsas() {
        if (items == null) return 0;
        int count = 0;
        for (ItemCarrito i : items) {
            if (i.esExtra()) {
                count += i.getCantidad();
            }
        }
        return count;
    }

    /**
     * Cuenta la cantidad de adicionales generales en el carrito.
     */
    public int contarAdicionales() {
        if (items == null) return 0;
        int count = 0;
        for (ItemCarrito i : items) {
            if (i.esAdicional()) {
                count += i.getCantidad();
            }
        }
        return count;
    }

    /**
     * Calcula el exceso de salsas/extras en el carrito de acuerdo con las porciones de alitas.
     * Es estrictamente proporcional: permitidas = alitas * maxSalsasPorAlita.
     */
    public int calcularExcesoSalsas(int maxSalsasPorAlita) {
        int permitidas = contarAlitas() * Math.max(0, maxSalsasPorAlita);
        int totalSalsas = contarExtrasSalsas();
        return Math.max(0, totalSalsas - permitidas);
    }

    /**
     * Calcula el exceso de adicionales en el carrito de acuerdo con los platos principales.
     * Es estrictamente proporcional: permitidos = principales * maxAdicionalesPorPrincipal.
     */
    public int calcularExcesoAdicionales(int maxAdicionalesPorPrincipal) {
        int permitidos = contarPlatosPrincipales() * Math.max(0, maxAdicionalesPorPrincipal);
        int totalAdicionales = contarAdicionales();
        return Math.max(0, totalAdicionales - permitidos);
    }

    /**
     * Determina si el pedido presenta algún exceso (en salsas o adicionales).
     */
    public boolean tieneExcesos(int maxSalsasPorAlita, int maxAdicionalesPorPrincipal) {
        return calcularExcesoSalsas(maxSalsasPorAlita) > 0 || calcularExcesoAdicionales(maxAdicionalesPorPrincipal) > 0;
    }

    public boolean tieneExtrasSalsas() {
        return contarExtrasSalsas() > 0;
    }

    /**
     * Determina si el carrito contiene productos de tipo adicional.
     */
    public boolean tieneAdicionales() {
        return contarAdicionales() > 0;
    }

    /**
     * Agrega un producto de forma individual (uno a uno),
     * preservando notas y salsas específicas de cada plato.
     */
    public void agregarItem(ItemCarrito nuevo) {
        if (nuevo == null)
            return;
        items.add(nuevo);
    }

    /**
     * Cuenta cuántas unidades de un mismo producto ya se han añadido al carrito
     * para validar contra el stock disponible.
     */
    public int contarPorProducto(Integer idProducto) {
        if (idProducto == null)
            return 0;
        int count = 0;
        for (ItemCarrito i : items) {
            if (idProducto.equals(i.getIdProducto())) {
                count++;
            }
        }
        return count;
    }

    public void eliminarItem(String idItem) {
        if (idItem == null)
            return;
        items.removeIf(item -> item.getIdItem().equals(idItem));
    }

    public void actualizarNota(String idItem, String nota) {
        if (idItem == null)
            return;
        for (ItemCarrito item : items) {
            if (item.getIdItem().equals(idItem)) {
                item.setNota(nota);
                return;
            }
        }
    }

    public void limpiar() {
        items.clear();
    }

    public Integer getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Integer idMesa) {
        this.idMesa = idMesa;
    }

    public Integer getNumeroMesa() {
        return numeroMesa;
    }

    public void setNumeroMesa(Integer numeroMesa) {
        this.numeroMesa = numeroMesa;
    }

    public String getSalon() {
        return salon;
    }

    public void setSalon(String salon) {
        this.salon = salon;
    }

    public List<ItemCarrito> getItems() {
        return items;
    }

    public void setItems(List<ItemCarrito> items) {
        this.items = items;
    }
}
