package com.cibertec.chilpayate.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReporteAnaliticoDto {

    // KPIs Generales
    private BigDecimal totalVentas = BigDecimal.ZERO;
    private BigDecimal totalVentasCobradas = BigDecimal.ZERO;
    private BigDecimal totalGastos = BigDecimal.ZERO;
    private BigDecimal utilidadNeta = BigDecimal.ZERO;
    private long totalComandas = 0;
    private BigDecimal ticketPromedio = BigDecimal.ZERO;
    private long totalPlatosDespachados = 0;

    // KPIs Especial Alitas & Personal El Chilpayate
    private long totalPorcionesAlitas = 0;
    private long totalPiezasAlitas = 0;
    private String platoMasVendido = "Sin ventas";
    private long platoMasVendidoCant = 0;
    private String alitaMasVendida = "Sin ventas";
    private long alitaMasVendidaCant = 0;
    private long alitaMasVendidaPiezas = 0;
    private String salsaMasPedida = "Sin registros";
    private long salsaMasPedidaCant = 0;
    private String mozoEstrella = "Sin registros";
    private long mozoEstrellaPlatos = 0;
    private BigDecimal mozoEstrellaMonto = BigDecimal.ZERO;
    private String horaPico = "-";
    private long horaPicoPedidos = 0;

    // Listados analíticos
    private List<PlatoVendidoDto> topPlatos = new ArrayList<>();
    private List<AlitaVendidoDto> topAlitas = new ArrayList<>();
    private List<SalsaTopDto> topSalsas = new ArrayList<>();
    private List<MozoRendimientoDto> rankingMozos = new ArrayList<>();
    private List<CanalVentaDto> ventasPorCanal = new ArrayList<>();
    private List<MetodoPagoMixDto> mixMetodosPago = new ArrayList<>();
    private List<HorarioPicoDto> horasPunta = new ArrayList<>();

    public ReporteAnaliticoDto() {}

    // Sub-DTO: Plato Más Vendido General
    public static class PlatoVendidoDto {
        private Integer idProducto;
        private String nombre;
        private String categoria;
        private long cantidad;
        private BigDecimal recaudacion = BigDecimal.ZERO;
        private double porcentaje;

        public PlatoVendidoDto() {}
        public PlatoVendidoDto(Integer idProducto, String nombre, String categoria, long cantidad, BigDecimal recaudacion, double porcentaje) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.categoria = categoria;
            this.cantidad = cantidad;
            this.recaudacion = recaudacion;
            this.porcentaje = porcentaje;
        }

        public Integer getIdProducto() { return idProducto; }
        public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public long getCantidad() { return cantidad; }
        public void setCantidad(long cantidad) { this.cantidad = cantidad; }
        public BigDecimal getRecaudacion() { return recaudacion; }
        public void setRecaudacion(BigDecimal recaudacion) { this.recaudacion = recaudacion; }
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    }

    // Sub-DTO: Especial de Alitas
    public static class AlitaVendidoDto {
        private Integer idProducto;
        private String nombre;
        private long cantidad; // porciones vendidas
        private long piezasEstimadas; // total alitas individuales
        private BigDecimal recaudacion = BigDecimal.ZERO;
        private double porcentaje;

        public AlitaVendidoDto() {}
        public AlitaVendidoDto(Integer idProducto, String nombre, long cantidad, long piezasEstimadas, BigDecimal recaudacion, double porcentaje) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.piezasEstimadas = piezasEstimadas;
            this.recaudacion = recaudacion;
            this.porcentaje = porcentaje;
        }

        public Integer getIdProducto() { return idProducto; }
        public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public long getCantidad() { return cantidad; }
        public void setCantidad(long cantidad) { this.cantidad = cantidad; }
        public long getPiezasEstimadas() { return piezasEstimadas; }
        public void setPiezasEstimadas(long piezasEstimadas) { this.piezasEstimadas = piezasEstimadas; }
        public BigDecimal getRecaudacion() { return recaudacion; }
        public void setRecaudacion(BigDecimal recaudacion) { this.recaudacion = recaudacion; }
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    }

    // Sub-DTO: Top Salsas
    public static class SalsaTopDto {
        private Integer idSalsa;
        private String nombre;
        private long vecesPedida;
        private double porcentaje;

        public SalsaTopDto() {}
        public SalsaTopDto(Integer idSalsa, String nombre, long vecesPedida, double porcentaje) {
            this.idSalsa = idSalsa;
            this.nombre = nombre;
            this.vecesPedida = vecesPedida;
            this.porcentaje = porcentaje;
        }

        public Integer getIdSalsa() { return idSalsa; }
        public void setIdSalsa(Integer idSalsa) { this.idSalsa = idSalsa; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public long getVecesPedida() { return vecesPedida; }
        public void setVecesPedida(long vecesPedida) { this.vecesPedida = vecesPedida; }
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    }

    // Sub-DTO: Rendimiento Mozo / Personal
    public static class MozoRendimientoDto {
        private Integer idUsuario;
        private String nombreCompleto;
        private String username;
        private String rol;
        private long pedidosAtendidos;
        private long platosVendidos;
        private BigDecimal totalVendido = BigDecimal.ZERO;
        private BigDecimal ticketPromedio = BigDecimal.ZERO;

        public MozoRendimientoDto() {}
        public MozoRendimientoDto(Integer idUsuario, String nombreCompleto, String username, String rol, long pedidosAtendidos, long platosVendidos, BigDecimal totalVendido, BigDecimal ticketPromedio) {
            this.idUsuario = idUsuario;
            this.nombreCompleto = nombreCompleto;
            this.username = username;
            this.rol = rol;
            this.pedidosAtendidos = pedidosAtendidos;
            this.platosVendidos = platosVendidos;
            this.totalVendido = totalVendido;
            this.ticketPromedio = ticketPromedio;
        }

        public Integer getIdUsuario() { return idUsuario; }
        public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
        public String getNombreCompleto() { return nombreCompleto; }
        public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
        public long getPedidosAtendidos() { return pedidosAtendidos; }
        public void setPedidosAtendidos(long pedidosAtendidos) { this.pedidosAtendidos = pedidosAtendidos; }
        public long getPlatosVendidos() { return platosVendidos; }
        public void setPlatosVendidos(long platosVendidos) { this.platosVendidos = platosVendidos; }
        public BigDecimal getTotalVendido() { return totalVendido; }
        public void setTotalVendido(BigDecimal totalVendido) { this.totalVendido = totalVendido; }
        public BigDecimal getTicketPromedio() { return ticketPromedio; }
        public void setTicketPromedio(BigDecimal ticketPromedio) { this.ticketPromedio = ticketPromedio; }
    }

    // Sub-DTO: Canales de Venta (Salón vs Delivery)
    public static class CanalVentaDto {
        private String canal; // "SALON", "DELIVERY"
        private String canalLabel; // "Salón (Mesas)", "Delivery"
        private String icon; // "▦", "🛵"
        private long cantidadPedidos;
        private BigDecimal totalMonto = BigDecimal.ZERO;
        private BigDecimal ticketPromedio = BigDecimal.ZERO;
        private double porcentaje;

        public CanalVentaDto() {}
        public CanalVentaDto(String canal, String canalLabel, String icon, long cantidadPedidos, BigDecimal totalMonto, BigDecimal ticketPromedio, double porcentaje) {
            this.canal = canal;
            this.canalLabel = canalLabel;
            this.icon = icon;
            this.cantidadPedidos = cantidadPedidos;
            this.totalMonto = totalMonto;
            this.ticketPromedio = ticketPromedio;
            this.porcentaje = porcentaje;
        }

        public String getCanal() { return canal; }
        public void setCanal(String canal) { this.canal = canal; }
        public String getCanalLabel() { return canalLabel; }
        public void setCanalLabel(String canalLabel) { this.canalLabel = canalLabel; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public long getCantidadPedidos() { return cantidadPedidos; }
        public void setCantidadPedidos(long cantidadPedidos) { this.cantidadPedidos = cantidadPedidos; }
        public BigDecimal getTotalMonto() { return totalMonto; }
        public void setTotalMonto(BigDecimal totalMonto) { this.totalMonto = totalMonto; }
        public BigDecimal getTicketPromedio() { return ticketPromedio; }
        public void setTicketPromedio(BigDecimal ticketPromedio) { this.ticketPromedio = ticketPromedio; }
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    }

    // Sub-DTO: Mix de Métodos de Pago
    public static class MetodoPagoMixDto {
        private String metodo;
        private BigDecimal totalMonto = BigDecimal.ZERO;
        private long transacciones;
        private double porcentaje;

        public MetodoPagoMixDto() {}
        public MetodoPagoMixDto(String metodo, BigDecimal totalMonto, long transacciones, double porcentaje) {
            this.metodo = metodo;
            this.totalMonto = totalMonto;
            this.transacciones = transacciones;
            this.porcentaje = porcentaje;
        }

        public String getMetodo() { return metodo; }
        public void setMetodo(String metodo) { this.metodo = metodo; }
        public BigDecimal getTotalMonto() { return totalMonto; }
        public void setTotalMonto(BigDecimal totalMonto) { this.totalMonto = totalMonto; }
        public long getTransacciones() { return transacciones; }
        public void setTransacciones(long transacciones) { this.transacciones = transacciones; }
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    }

    // Sub-DTO: Franja Horaria / Horarios Pico
    public static class HorarioPicoDto {
        private int hora;
        private String rangoHorario;
        private long cantidadPedidos;
        private BigDecimal totalMonto = BigDecimal.ZERO;
        private double porcentaje;

        public HorarioPicoDto() {}
        public HorarioPicoDto(int hora, String rangoHorario, long cantidadPedidos, BigDecimal totalMonto, double porcentaje) {
            this.hora = hora;
            this.rangoHorario = rangoHorario;
            this.cantidadPedidos = cantidadPedidos;
            this.totalMonto = totalMonto;
            this.porcentaje = porcentaje;
        }

        public int getHora() { return hora; }
        public void setHora(int hora) { this.hora = hora; }
        public String getRangoHorario() { return rangoHorario; }
        public void setRangoHorario(String rangoHorario) { this.rangoHorario = rangoHorario; }
        public long getCantidadPedidos() { return cantidadPedidos; }
        public void setCantidadPedidos(long cantidadPedidos) { this.cantidadPedidos = cantidadPedidos; }
        public BigDecimal getTotalMonto() { return totalMonto; }
        public void setTotalMonto(BigDecimal totalMonto) { this.totalMonto = totalMonto; }
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
    }

    // Getters y Setters de ReporteAnaliticoDto
    public BigDecimal getTotalVentas() { return totalVentas; }
    public void setTotalVentas(BigDecimal totalVentas) { this.totalVentas = totalVentas; }
    public BigDecimal getTotalVentasCobradas() { return totalVentasCobradas; }
    public void setTotalVentasCobradas(BigDecimal totalVentasCobradas) { this.totalVentasCobradas = totalVentasCobradas; }
    public BigDecimal getTotalGastos() { return totalGastos; }
    public void setTotalGastos(BigDecimal totalGastos) { this.totalGastos = totalGastos; }
    public BigDecimal getUtilidadNeta() { return utilidadNeta; }
    public void setUtilidadNeta(BigDecimal utilidadNeta) { this.utilidadNeta = utilidadNeta; }
    public long getTotalComandas() { return totalComandas; }
    public void setTotalComandas(long totalComandas) { this.totalComandas = totalComandas; }
    public BigDecimal getTicketPromedio() { return ticketPromedio; }
    public void setTicketPromedio(BigDecimal ticketPromedio) { this.ticketPromedio = ticketPromedio; }
    public long getTotalPlatosDespachados() { return totalPlatosDespachados; }
    public void setTotalPlatosDespachados(long totalPlatosDespachados) { this.totalPlatosDespachados = totalPlatosDespachados; }

    public long getTotalPorcionesAlitas() { return totalPorcionesAlitas; }
    public void setTotalPorcionesAlitas(long totalPorcionesAlitas) { this.totalPorcionesAlitas = totalPorcionesAlitas; }
    public long getTotalPiezasAlitas() { return totalPiezasAlitas; }
    public void setTotalPiezasAlitas(long totalPiezasAlitas) { this.totalPiezasAlitas = totalPiezasAlitas; }
    public String getPlatoMasVendido() { return platoMasVendido; }
    public void setPlatoMasVendido(String platoMasVendido) { this.platoMasVendido = platoMasVendido; }
    public long getPlatoMasVendidoCant() { return platoMasVendidoCant; }
    public void setPlatoMasVendidoCant(long platoMasVendidoCant) { this.platoMasVendidoCant = platoMasVendidoCant; }
    public String getAlitaMasVendida() { return alitaMasVendida; }
    public void setAlitaMasVendida(String alitaMasVendida) { this.alitaMasVendida = alitaMasVendida; }
    public long getAlitaMasVendidaCant() { return alitaMasVendidaCant; }
    public void setAlitaMasVendidaCant(long alitaMasVendidaCant) { this.alitaMasVendidaCant = alitaMasVendidaCant; }
    public long getAlitaMasVendidaPiezas() { return alitaMasVendidaPiezas; }
    public void setAlitaMasVendidaPiezas(long alitaMasVendidaPiezas) { this.alitaMasVendidaPiezas = alitaMasVendidaPiezas; }
    public String getSalsaMasPedida() { return salsaMasPedida; }
    public void setSalsaMasPedida(String salsaMasPedida) { this.salsaMasPedida = salsaMasPedida; }
    public long getSalsaMasPedidaCant() { return salsaMasPedidaCant; }
    public void setSalsaMasPedidaCant(long salsaMasPedidaCant) { this.salsaMasPedidaCant = salsaMasPedidaCant; }
    public String getMozoEstrella() { return mozoEstrella; }
    public void setMozoEstrella(String mozoEstrella) { this.mozoEstrella = mozoEstrella; }
    public long getMozoEstrellaPlatos() { return mozoEstrellaPlatos; }
    public void setMozoEstrellaPlatos(long mozoEstrellaPlatos) { this.mozoEstrellaPlatos = mozoEstrellaPlatos; }
    public BigDecimal getMozoEstrellaMonto() { return mozoEstrellaMonto; }
    public void setMozoEstrellaMonto(BigDecimal mozoEstrellaMonto) { this.mozoEstrellaMonto = mozoEstrellaMonto; }
    public String getHoraPico() { return horaPico; }
    public void setHoraPico(String horaPico) { this.horaPico = horaPico; }
    public long getHoraPicoPedidos() { return horaPicoPedidos; }
    public void setHoraPicoPedidos(long horaPicoPedidos) { this.horaPicoPedidos = horaPicoPedidos; }

    public List<PlatoVendidoDto> getTopPlatos() { return topPlatos; }
    public void setTopPlatos(List<PlatoVendidoDto> topPlatos) { this.topPlatos = topPlatos; }
    public List<AlitaVendidoDto> getTopAlitas() { return topAlitas; }
    public void setTopAlitas(List<AlitaVendidoDto> topAlitas) { this.topAlitas = topAlitas; }
    public List<SalsaTopDto> getTopSalsas() { return topSalsas; }
    public void setTopSalsas(List<SalsaTopDto> topSalsas) { this.topSalsas = topSalsas; }
    public List<MozoRendimientoDto> getRankingMozos() { return rankingMozos; }
    public void setRankingMozos(List<MozoRendimientoDto> rankingMozos) { this.rankingMozos = rankingMozos; }
    public List<CanalVentaDto> getVentasPorCanal() { return ventasPorCanal; }
    public void setVentasPorCanal(List<CanalVentaDto> ventasPorCanal) { this.ventasPorCanal = ventasPorCanal; }
    public List<MetodoPagoMixDto> getMixMetodosPago() { return mixMetodosPago; }
    public void setMixMetodosPago(List<MetodoPagoMixDto> mixMetodosPago) { this.mixMetodosPago = mixMetodosPago; }
    public List<HorarioPicoDto> getHorasPunta() { return horasPunta; }
    public void setHorasPunta(List<HorarioPicoDto> horasPunta) { this.horasPunta = horasPunta; }
}
