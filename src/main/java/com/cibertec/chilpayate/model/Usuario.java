package com.cibertec.chilpayate.model;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Integer idUsuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @OneToMany(mappedBy = "usuario")
    private List<MovimientoStock> movimientoStock;

    @ManyToOne
    @JoinColumn(name = "idRol", nullable = false)
    private Rol rol;

    @OneToMany(mappedBy = "usuario")
    private List<Pedido> pedidos;

    @OneToMany(mappedBy = "usuario")
    private List<Venta> ventas;

    @OneToMany(mappedBy = "usuario")
    private List<Incidente> incidentes;

    @OneToMany(mappedBy = "admin")
    private List<Incidente> incidentesAprobados;

    @OneToMany(mappedBy = "usuario")
    private List<CierreCaja> cierreCaja;

    public Usuario() {
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public List<MovimientoStock> getMovimientoStock() {
        return movimientoStock;
    }

    public void setMovimientoStock(List<MovimientoStock> movimientoStock) {
        this.movimientoStock = movimientoStock;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<Pedido> pedidos) {
        this.pedidos = pedidos;
    }

    public List<Venta> getVentas() {
        return ventas;
    }

    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }

    public List<Incidente> getIncidentes() {
        return incidentes;
    }

    public void setIncidentes(List<Incidente> incidentes) {
        this.incidentes = incidentes;
    }

    public List<Incidente> getIncidentesAprobados() {
        return incidentesAprobados;
    }

    public void setIncidentesAprobados(List<Incidente> incidentesAprobados) {
        this.incidentesAprobados = incidentesAprobados;
    }

    public List<CierreCaja> getCierreCaja() {
        return cierreCaja;
    }

    public void setCierreCaja(List<CierreCaja> cierreCaja) {
        this.cierreCaja = cierreCaja;
    }

}