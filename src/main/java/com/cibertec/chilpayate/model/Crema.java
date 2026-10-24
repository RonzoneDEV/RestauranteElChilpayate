package com.cibertec.chilpayate.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "Crema")
public class Crema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCrema")
    private Integer idCrema;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
    
    @OneToMany(mappedBy = "crema")
    private List<DetallePedidoCrema> detallePedidoCrema;


	public Crema() {}

    public Integer getIdCrema() { return idCrema; }
    public void setIdCrema(Integer idCrema) { this.idCrema = idCrema; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public List<DetallePedidoCrema> getDetallePedidoCrema() { return detallePedidoCrema; }

	public void setDetallePedidoCrema(List<DetallePedidoCrema> detallePedidoCrema) { this.detallePedidoCrema = detallePedidoCrema; }
}