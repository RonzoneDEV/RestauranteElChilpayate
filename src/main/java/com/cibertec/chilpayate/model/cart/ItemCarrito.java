package com.cibertec.chilpayate.model.cart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemCarrito implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idItem;
    private Integer idProducto;
    private String nombreProducto;
    private String categoriaNombre;
    private com.cibertec.chilpayate.model.TipoProducto tipo = com.cibertec.chilpayate.model.TipoProducto.PRINCIPAL;
    private BigDecimal precioUnitario;
    private String nota;
    private List<Integer> idCremas = new ArrayList<>();
    private List<String> nombresCremas = new ArrayList<>();
    private List<Integer> idSalsas = new ArrayList<>();
    private List<String> nombresSalsas = new ArrayList<>();

    public ItemCarrito() {
        this.idItem = UUID.randomUUID().toString();
        this.precioUnitario = BigDecimal.ZERO;
        this.nota = "";
    }

    public ItemCarrito(Integer idProducto, String nombreProducto, String categoriaNombre,
                       BigDecimal precioUnitario, String nota,
                       List<Integer> idSalsas, List<String> nombresSalsas) {
        this(idProducto, nombreProducto, categoriaNombre, com.cibertec.chilpayate.model.TipoProducto.PRINCIPAL, precioUnitario, nota, null, null, idSalsas, nombresSalsas);
    }

    public ItemCarrito(Integer idProducto, String nombreProducto, String categoriaNombre,
                       com.cibertec.chilpayate.model.TipoProducto tipo,
                       BigDecimal precioUnitario, String nota,
                       List<Integer> idSalsas, List<String> nombresSalsas) {
        this(idProducto, nombreProducto, categoriaNombre, tipo, precioUnitario, nota, null, null, idSalsas, nombresSalsas);
    }

    public ItemCarrito(Integer idProducto, String nombreProducto, String categoriaNombre,
                       BigDecimal precioUnitario, String nota,
                       List<Integer> idCremas, List<String> nombresCremas,
                       List<Integer> idSalsas, List<String> nombresSalsas) {
        this(idProducto, nombreProducto, categoriaNombre, com.cibertec.chilpayate.model.TipoProducto.PRINCIPAL, precioUnitario, nota, idCremas, nombresCremas, idSalsas, nombresSalsas);
    }

    public ItemCarrito(Integer idProducto, String nombreProducto, String categoriaNombre,
                       com.cibertec.chilpayate.model.TipoProducto tipo,
                       BigDecimal precioUnitario, String nota,
                       List<Integer> idCremas, List<String> nombresCremas,
                       List<Integer> idSalsas, List<String> nombresSalsas) {
        this.idItem = UUID.randomUUID().toString();
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.categoriaNombre = categoriaNombre;
        this.tipo = (tipo != null) ? tipo : com.cibertec.chilpayate.model.TipoProducto.PRINCIPAL;
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        this.nota = nota != null ? nota : "";
        if (idCremas != null) this.idCremas = new ArrayList<>(idCremas);
        if (nombresCremas != null) this.nombresCremas = new ArrayList<>(nombresCremas);
        if (idSalsas != null) this.idSalsas = new ArrayList<>(idSalsas);
        if (nombresSalsas != null) this.nombresSalsas = new ArrayList<>(nombresSalsas);
    }

    public BigDecimal getSubtotal() {
        return precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }

    public int getCantidad() {
        return 1;
    }

    public String getIdItem() {
        return idItem;
    }

    public void setIdItem(String idItem) {
        this.idItem = idItem;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public List<Integer> getIdCremas() {
        return idCremas;
    }

    public void setIdCremas(List<Integer> idCremas) {
        this.idCremas = idCremas;
    }

    public List<String> getNombresCremas() {
        return nombresCremas;
    }

    public void setNombresCremas(List<String> nombresCremas) {
        this.nombresCremas = nombresCremas;
    }

    public List<Integer> getIdSalsas() {
        return idSalsas;
    }

    public void setIdSalsas(List<Integer> idSalsas) {
        this.idSalsas = idSalsas;
    }

    public List<String> getNombresSalsas() {
        return nombresSalsas;
    }

    public void setNombresSalsas(List<String> nombresSalsas) {
        this.nombresSalsas = nombresSalsas;
    }

    public com.cibertec.chilpayate.model.TipoProducto getTipo() {
        return tipo;
    }

    public void setTipo(com.cibertec.chilpayate.model.TipoProducto tipo) {
        this.tipo = tipo;
    }

    public boolean esAlitas() {
        if (categoriaNombre != null && categoriaNombre.toUpperCase().contains("ALITA")) {
            return true;
        }
        return nombreProducto != null && nombreProducto.toLowerCase().contains("alita");
    }

    public boolean esPrincipal() {
        if (tipo == com.cibertec.chilpayate.model.TipoProducto.PRINCIPAL) {
            return true;
        }
        if (categoriaNombre != null) {
            String cat = categoriaNombre.toUpperCase();
            return !cat.contains("SALSA") && !cat.contains("EXTRA") && !cat.contains("ADICIONAL")
                    && !cat.contains("BEBIDA") && !cat.contains("GASEOSA") && !cat.contains("REFRESCO");
        }
        return false;
    }

    public boolean esExtra() {
        if (tipo == com.cibertec.chilpayate.model.TipoProducto.EXTRA) {
            return true;
        }
        if (categoriaNombre != null) {
            String cat = categoriaNombre.toUpperCase();
            return cat.contains("SALSA") || cat.contains("EXTRA");
        }
        return false;
    }

    public boolean esAdicional() {
        if (tipo == com.cibertec.chilpayate.model.TipoProducto.ADICIONAL) {
            return true;
        }
        if (categoriaNombre != null) {
            String cat = categoriaNombre.toUpperCase();
            return (cat.contains("ADICIONAL") || cat.contains("CREMA")) && !esExtra();
        }
        return false;
    }

    public boolean esIntermedio() {
        if (tipo == com.cibertec.chilpayate.model.TipoProducto.INTERMEDIO) {
            return true;
        }
        if (categoriaNombre != null) {
            String cat = categoriaNombre.toUpperCase();
            return cat.contains("BEBIDA") || cat.contains("GASEOSA") || cat.contains("REFRESCO");
        }
        return false;
    }
}
