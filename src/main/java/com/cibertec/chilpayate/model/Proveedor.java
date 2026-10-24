package com.cibertec.chilpayate.model;

import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "Proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProveedor")
    private Integer idProveedor;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 140, message = "La razón social no puede exceder 140 caracteres")
    @Column(name = "razonSocial", nullable = false, length = 140)
    private String razonSocial;

    @Pattern(regexp = "^$|[0-9]{11}", message = "El RUC debe tener 11 dígitos numéricos")
    @Column(name = "ruc", length = 11)
    private String ruc;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 180, message = "La dirección no puede exceder 180 caracteres")
    @Column(name = "direccion", length = 180)
    private String direccion;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
    
    @OneToMany(mappedBy = "proveedor")
    private List<Gasto> gastos;

    public Proveedor() {}

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

	public List<Gasto> getGastos() { return gastos; }
	public void setGastos(List<Gasto> gastos) { this.gastos = gastos; }
    
    
}