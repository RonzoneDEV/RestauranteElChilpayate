package com.cibertec.chilpayate.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "MetodoPago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMetodoPago")
    private Integer idMetodoPago;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
    
    @OneToMany(mappedBy = "metodoPago")
    private List<PagoMedio> pagoMedio;

    public MetodoPago() {}

    public Integer getIdMetodoPago() { return idMetodoPago; }
    public void setIdMetodoPago(Integer idMetodoPago) { this.idMetodoPago = idMetodoPago; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

	public List<PagoMedio> getPagoMedio() { return pagoMedio; }
	public void setPagoMedio(List<PagoMedio> pagoMedio) { this.pagoMedio = pagoMedio; }
    
}