package com.cibertec.chilpayate.model;

import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "Mesa", uniqueConstraints = {
    @UniqueConstraint(name = "uk_mesa_numero_salon", columnNames = {"numero", "Salon"})
})
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMesa")
    private Integer idMesa;

    @NotNull(message = "El número de mesa es obligatorio")
    @Min(value = 1, message = "El número de mesa debe ser al menos 1")
    @Column(name = "numero", nullable = false)
    private Integer numero;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es de 1 persona")
    @Column(name = "capacidad", nullable = false)
    private Integer capacidad = 4;

    @NotBlank(message = "El salón o área es obligatorio")
    @Size(max = 50, message = "El salón no puede exceder 50 caracteres")
    @Column(name = "Salon", nullable = false, length = 50)
    private String salon = "Salón Principal";

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoMesa estado = EstadoMesa.LIBRE;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
    
    @OneToMany(mappedBy = "mesa")
    private List<Pedido> pedidos;

    public Mesa() {}

    public Integer getIdMesa() { return idMesa; }
    public void setIdMesa(Integer idMesa) { this.idMesa = idMesa; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public String getSalon() { return salon; }
    public void setSalon(String salon) { this.salon = salon; }

    public EstadoMesa getEstado() { return estado; }
    public void setEstado(EstadoMesa estado) { this.estado = estado; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public enum EstadoMesa {
        LIBRE, OCUPADA, BLOQUEADA
    }
    
    public List<Pedido> getPedidos() { return pedidos; }
    public void setPedidos(List<Pedido> pedidos) { this.pedidos = pedidos; }
    
    
}