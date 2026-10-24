package com.cibertec.chilpayate.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "Categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCategoria", nullable = false)
    private Integer idCategoria;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "maxExtras")
    private Integer maxExtras = 2;

    @Column(name = "maxAdicionales")
    private Integer maxAdicionales = 2;
    
    @OneToMany(mappedBy = "categoria")
    private List<Producto> productos;

    public Categoria() {}

    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public Integer getMaxExtras() { return maxExtras != null ? maxExtras : 2; }
    public void setMaxExtras(Integer maxExtras) { this.maxExtras = maxExtras; }

    public Integer getMaxAdicionales() { return maxAdicionales != null ? maxAdicionales : 2; }
    public void setMaxAdicionales(Integer maxAdicionales) { this.maxAdicionales = maxAdicionales; }
    
    public List<Producto> getProductos() { return productos; }
    public void setProductos(List<Producto> productos) { this.productos = productos; }
}