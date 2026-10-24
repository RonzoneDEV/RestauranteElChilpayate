package com.cibertec.chilpayate.model.cart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CarritoDelivery implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ItemCarrito> items = new ArrayList<>();
    private String nombreCliente = "";
    private String telefono = "";
    private String direccion = "";
    private String referencia = "";
    private BigDecimal costoDelivery = new BigDecimal("5.00");
    private String repartidor = "Por asignar";

    public CarritoDelivery() {
    }

    public List<ItemCarrito> getItems() {
        return items;
    }

    public void setItems(List<ItemCarrito> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void agregarItem(ItemCarrito item) {
        if (item != null) {
            this.items.add(item);
        }
    }

    /**
     * Determina si el carrito delivery ya contiene al menos un plato principal.
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
     * Proporcional: permitidas = alitas * maxSalsasPorAlita.
     */
    public int calcularExcesoSalsas(int maxSalsasPorAlita) {
        int permitidas = contarAlitas() * Math.max(0, maxSalsasPorAlita);
        int totalSalsas = contarExtrasSalsas();
        return Math.max(0, totalSalsas - permitidas);
    }

    /**
     * Calcula el exceso de adicionales en el carrito de acuerdo con los platos principales.
     * Proporcional: permitidos = principales * maxAdicionalesPorPrincipal.
     */
    public int calcularExcesoAdicionales(int maxAdicionalesPorPrincipal) {
        int permitidos = contarPlatosPrincipales() * Math.max(0, maxAdicionalesPorPrincipal);
        int totalAdicionales = contarAdicionales();
        return Math.max(0, totalAdicionales - permitidos);
    }

    /**
     * Determina si el pedido delivery presenta algún exceso (en salsas o adicionales).
     */
    public boolean tieneExcesos(int maxSalsasPorAlita, int maxAdicionalesPorPrincipal) {
        return calcularExcesoSalsas(maxSalsasPorAlita) > 0 || calcularExcesoAdicionales(maxAdicionalesPorPrincipal) > 0;
    }

    public boolean tieneExtrasSalsas() {
        return contarExtrasSalsas() > 0;
    }

    public boolean tieneAdicionales() {
        return contarAdicionales() > 0;
    }

    public void eliminarItem(String idItem) {
        if (idItem != null) {
            this.items.removeIf(i -> idItem.equals(i.getIdItem()));
        }
    }

    public void actualizarNota(String idItem, String nota) {
        if (idItem != null) {
            for (ItemCarrito item : items) {
                if (idItem.equals(item.getIdItem())) {
                    item.setNota(nota);
                    break;
                }
            }
        }
    }

    public int contarPorProducto(Integer idProducto) {
        if (idProducto == null) return 0;
        int count = 0;
        for (ItemCarrito item : items) {
            if (idProducto.equals(item.getIdProducto())) {
                count += item.getCantidad();
            }
        }
        return count;
    }

    public int getCantidadTotal() {
        int count = 0;
        for (ItemCarrito item : items) {
            count += item.getCantidad();
        }
        return count;
    }

    public BigDecimal getSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            sum = sum.add(item.getSubtotal());
        }
        return sum;
    }

    public BigDecimal getTotal() {
        BigDecimal sub = getSubtotal();
        BigDecimal costo = costoDelivery != null ? costoDelivery : BigDecimal.ZERO;
        return sub.add(costo);
    }

    public boolean estaVacio() {
        return items == null || items.isEmpty();
    }

    public void limpiar() {
        this.items.clear();
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public BigDecimal getCostoDelivery() {
        return costoDelivery;
    }

    public void setCostoDelivery(BigDecimal costoDelivery) {
        this.costoDelivery = costoDelivery != null ? costoDelivery : BigDecimal.ZERO;
    }

    public String getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(String repartidor) {
        this.repartidor = repartidor;
    }

    private String metodoPago = "YAPE";
    private String numeroOperacion = "";
    private boolean confirmarPagoAhora = true;

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public void setNumeroOperacion(String numeroOperacion) {
        this.numeroOperacion = numeroOperacion;
    }

    public boolean isConfirmarPagoAhora() {
        return confirmarPagoAhora;
    }

    public void setConfirmarPagoAhora(boolean confirmarPagoAhora) {
        this.confirmarPagoAhora = confirmarPagoAhora;
    }
}
