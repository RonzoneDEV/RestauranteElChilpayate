package com.cibertec.chilpayate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProducto")
    private Integer idProducto;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @NotNull(message = "Debe asignar una categoría al producto")
    @ManyToOne
    @JoinColumn(name = "idCategoria", nullable = false)
    private Categoria categoria;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "controlaStock", nullable = false)
    private Boolean controlaStock = true;

    @Column(name = "imagen", length = 255)
    private String imagen;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "permiteCrema")
    private Boolean permiteCrema = false;

    @Column(name = "permiteSalsa")
    private Boolean permiteSalsa = false;

    @Column(name = "limiteSalsa")
    private Integer limiteSalsa = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoProducto tipo;

    @OneToMany(mappedBy = "producto")
    private List<MovimientoStock> movimientoStock;

    @OneToMany(mappedBy = "producto")
    private List<DetallePedido> detallePedido;

    @OneToMany(mappedBy = "salsa")
    private List<DetallePedidoSalsa> detallePedidoSalsa;

    public Producto() {
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getControlaStock() {
        return controlaStock;
    }

    public void setControlaStock(Boolean controlaStock) {
        this.controlaStock = controlaStock;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public Boolean getPermiteCrema() {
        return permiteCrema;
    }

    public void setPermiteCrema(Boolean permiteCrema) {
        this.permiteCrema = permiteCrema;
    }

    public Boolean getPermiteSalsa() {
        return permiteSalsa;
    }

    public void setPermiteSalsa(Boolean permiteSalsa) {
        this.permiteSalsa = permiteSalsa;
    }

    public Integer getLimiteSalsa() {
        return limiteSalsa;
    }

    public void setLimiteSalsa(Integer limiteSalsa) {
        this.limiteSalsa = limiteSalsa;
    }

    public TipoProducto getTipo() {
        return tipo != null ? tipo : inferirTipo();
    }

    public void setTipo(TipoProducto tipo) {
        this.tipo = tipo;
    }

    public TipoProducto inferirTipo() {
        if (categoria != null && categoria.getNombre() != null) {
            String cat = categoria.getNombre().toUpperCase();
            if (cat.contains("SALSA") || cat.contains("EXTRA")) {
                return TipoProducto.EXTRA;
            }
            if (cat.contains("ADICIONAL") || cat.contains("CREMA")) {
                return TipoProducto.ADICIONAL;
            }
            if (cat.contains("BEBIDA") || cat.contains("GASEOSA") || cat.contains("REFRESCO")) {
                return TipoProducto.INTERMEDIO;
            }
        }
        return TipoProducto.PRINCIPAL;
    }

    public boolean esPrincipal() {
        return getTipo() == TipoProducto.PRINCIPAL;
    }

    public boolean esIntermedio() {
        return getTipo() == TipoProducto.INTERMEDIO;
    }

    public boolean esAdicional() {
        return getTipo() == TipoProducto.ADICIONAL;
    }

    public boolean esExtra() {
        return getTipo() == TipoProducto.EXTRA;
    }

    public boolean esAlitas() {
        if (categoria != null && categoria.getNombre() != null) {
            String cat = categoria.getNombre().toUpperCase();
            if (cat.contains("ALITA")) {
                return true;
            }
        }
        return nombre != null && nombre.toLowerCase().contains("alita");
    }

    public List<MovimientoStock> getMovimientoStock() {
        return movimientoStock;
    }

    public void setMovimientoStock(List<MovimientoStock> movimientoStock) {
        this.movimientoStock = movimientoStock;
    }

    public List<DetallePedido> getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(List<DetallePedido> detallePedido) {
        this.detallePedido = detallePedido;
    }

    public List<DetallePedidoSalsa> getDetallePedidoSalsa() {
        return detallePedidoSalsa;
    }

    public void setDetallePedidoSalsa(List<DetallePedidoSalsa> detallePedidoSalsa) {
        this.detallePedidoSalsa = detallePedidoSalsa;
    }
}