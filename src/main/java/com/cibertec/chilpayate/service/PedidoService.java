package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;
    private final DetallePedidoCremaRepository detalleCremaRepo;
    private final DetallePedidoSalsaRepository detalleSalsaRepo;
    private final MesaRepository mesaRepo;
    private final ProductoRepository productoRepo;
    private final CremaRepository cremaRepo;
    private final PedidoDeliveryRepository deliveryRepo;
    private final TicketCocinaRepository ticketRepo;
    private final MovimientoStockRepository stockRepo;
    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;

    public PedidoService(PedidoRepository pedidoRepo,
                         DetallePedidoRepository detalleRepo,
                         DetallePedidoCremaRepository detalleCremaRepo,
                         DetallePedidoSalsaRepository detalleSalsaRepo,
                         MesaRepository mesaRepo,
                         ProductoRepository productoRepo,
                         CremaRepository cremaRepo,
                         PedidoDeliveryRepository deliveryRepo,
                         TicketCocinaRepository ticketRepo,
                         MovimientoStockRepository stockRepo,
                         UsuarioRepository usuarioRepo,
                         ClienteRepository clienteRepo) {
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
        this.detalleCremaRepo = detalleCremaRepo;
        this.detalleSalsaRepo = detalleSalsaRepo;
        this.mesaRepo = mesaRepo;
        this.productoRepo = productoRepo;
        this.cremaRepo = cremaRepo;
        this.deliveryRepo = deliveryRepo;
        this.ticketRepo = ticketRepo;
        this.stockRepo = stockRepo;
        this.usuarioRepo = usuarioRepo;
        this.clienteRepo = clienteRepo;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<PedidoDelivery> listarTodosDelivery() {
        return deliveryRepo.findAllByOrderByFechaAsignacionDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PedidoDelivery> obtenerDeliveryPorId(Integer id) {
        return deliveryRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PedidoDelivery> obtenerDeliveryPorPedido(Integer idPedido) {
        return deliveryRepo.findByPedido_IdPedido(idPedido);
    }

    @Transactional
    public PedidoDelivery guardarDelivery(PedidoDelivery delivery) {
        return deliveryRepo.save(delivery);
    }

    @Transactional(readOnly = true)
    public int obtenerProximoNumeroTicket(Integer idPedido) {
        return ticketRepo.findByPedido_IdPedido(idPedido).size() + 1;
    }

    public List<Pedido> listarActivos() {
        return pedidoRepo.findByEstadoInOrderByFechaDesc(
                List.of(Pedido.EstadoPedido.ACTIVA, Pedido.EstadoPedido.PRECUENTA, Pedido.EstadoPedido.BORRADOR)
        );
    }

    public Optional<Pedido> obtenerPorId(Integer id) {
        return pedidoRepo.findById(id);
    }

    public Optional<Pedido> obtenerPorMesaActiva(Integer idMesa) {
        return pedidoRepo.findFirstByMesa_IdMesaAndEstadoInOrderByFechaDesc(
                idMesa, List.of(Pedido.EstadoPedido.ACTIVA, Pedido.EstadoPedido.PRECUENTA, Pedido.EstadoPedido.BORRADOR)
        );
    }

    public BigDecimal calcularTotal(Pedido p) {
        if (p == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detalles = (p.getIdPedido() != null)
                ? detalleRepo.findByPedido_IdPedido(p.getIdPedido())
                : p.getDetallePedido();
        if (detalles != null) {
            for (DetallePedido d : detalles) {
                if (d != null && d.getEstado() != DetallePedido.EstadoDetalle.ANULADO && d.getPrecioActual() != null) {
                    total = total.add(d.getPrecioActual());
                }
            }
        }
        if (p.getPedidoDelivery() != null && p.getPedidoDelivery().getCostoDelivery() != null) {
            total = total.add(p.getPedidoDelivery().getCostoDelivery());
        }
        return total;
    }

    public boolean tienePlatoPrincipal(Pedido p) {
        if (p == null || p.getDetallePedido() == null) return false;
        return p.getDetallePedido().stream()
                .filter(d -> d.getEstado() != DetallePedido.EstadoDetalle.ANULADO)
                .anyMatch(d -> d.getProducto() != null && d.getProducto().esPrincipal());
    }

    @Transactional
    public Pedido crearPedidoSalon(Integer idMesa, Usuario mozo, Cliente cliente, String notaInicial) {
        Mesa mesa = mesaRepo.findById(idMesa)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada: " + idMesa));

        Usuario managedMozo = null;
        if (mozo != null && mozo.getIdUsuario() != null) {
            managedMozo = usuarioRepo.findById(mozo.getIdUsuario()).orElse(mozo);
        }

        Cliente managedCliente = null;
        if (cliente != null && cliente.getIdCliente() != null) {
            managedCliente = clienteRepo.findById(cliente.getIdCliente()).orElse(cliente);
        }

        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDateTime.now());
        pedido.setTipo(Pedido.TipoPedido.SALON);
        pedido.setMesa(mesa);
        pedido.setUsuario(managedMozo);
        pedido.setCliente(managedCliente);
        pedido.setEstado(Pedido.EstadoPedido.ACTIVA);
        pedido.setCodigo("PED-" + (System.currentTimeMillis() % 10000000));

        mesa.setEstado(Mesa.EstadoMesa.OCUPADA);
        mesaRepo.save(mesa);

        return pedidoRepo.save(pedido);
    }

    @Transactional
    public Pedido crearPedidoDelivery(Usuario cajero, Cliente cliente, String direccion, String telefono,
                                      String referencia, BigDecimal costoDelivery, String repartidor) {
        Usuario managedCajero = null;
        if (cajero != null && cajero.getIdUsuario() != null) {
            managedCajero = usuarioRepo.findById(cajero.getIdUsuario()).orElse(cajero);
        }

        Cliente managedCliente = null;
        if (cliente != null && cliente.getIdCliente() != null) {
            managedCliente = clienteRepo.findById(cliente.getIdCliente()).orElse(cliente);
        }

        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDateTime.now());
        pedido.setTipo(Pedido.TipoPedido.DELIVERY);
        pedido.setUsuario(managedCajero);
        pedido.setCliente(managedCliente);
        pedido.setEstado(Pedido.EstadoPedido.ACTIVA);
        pedido.setCodigo("DEL-" + (System.currentTimeMillis() % 10000000));

        Pedido guardado = pedidoRepo.save(pedido);

        PedidoDelivery del = new PedidoDelivery();
        del.setPedido(guardado);
        del.setDireccion(direccion != null ? direccion : (managedCliente != null ? managedCliente.getDireccion() : ""));
        del.setTelefono(telefono != null ? telefono : (managedCliente != null ? managedCliente.getTelefono() : ""));
        del.setReferencia(referencia != null ? referencia : "");
        del.setCostoDelivery(costoDelivery != null ? costoDelivery : BigDecimal.ZERO);
        del.setFechaAsignacion(LocalDateTime.now());
        del.setEstado(PedidoDelivery.EstadoDelivery.EN_PREPARACION);
        del.setRepartidor(repartidor != null ? repartidor : "Sin asignar");
        deliveryRepo.save(del);

        guardado.setPedidoDelivery(del);
        return guardado;
    }

    @Transactional
    public DetallePedido agregarItem(Integer idPedido, Integer idProducto, List<Integer> idCremas,
                                     List<Integer> idSalsas, String nota) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));
        Producto prod = productoRepo.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));

        DetallePedido det = new DetallePedido();
        det.setPedido(pedido);
        det.setProducto(prod);
        det.setPrecioActual(prod.getPrecio());
        det.setNota(nota);
        det.setEstado(DetallePedido.EstadoDetalle.BORRADOR);

        DetallePedido guardado = detalleRepo.save(det);

        // Cremas seleccionadas
        if (idCremas != null) {
            for (Integer idCrema : idCremas) {
                cremaRepo.findById(idCrema).ifPresent(c -> {
                    DetallePedidoCrema dpc = new DetallePedidoCrema();
                    dpc.setDetallePedido(guardado);
                    dpc.setCrema(c);
                    detalleCremaRepo.save(dpc);
                });
            }
        }

        // Salsas seleccionadas (son Productos donde permiteSalsa=true)
        if (idSalsas != null) {
            for (Integer idSalsa : idSalsas) {
                productoRepo.findById(idSalsa).ifPresent(salsa -> {
                    DetallePedidoSalsa dps = new DetallePedidoSalsa();
                    dps.setDetallePedido(guardado);
                    dps.setSalsa(salsa);
                    detalleSalsaRepo.save(dps);
                });
            }
        }

        // Control de stock si aplica
        if (Boolean.TRUE.equals(prod.getControlaStock()) && prod.getStock() != null) {
            int anterior = prod.getStock();
            int nuevo = Math.max(0, anterior - 1);
            prod.setStock(nuevo);
            productoRepo.save(prod);

            MovimientoStock mov = new MovimientoStock();
            mov.setProducto(prod);
            mov.setTipoMovimiento(MovimientoStock.TipoMovimiento.SALIDA);
            mov.setCantidad(1);
            mov.setStockAnterior(anterior);
            mov.setStockNuevo(nuevo);
            mov.setMotivo("Venta en Pedido #" + pedido.getIdPedido());
            mov.setFechaHora(LocalDateTime.now());
            mov.setUsuario(pedido.getUsuario());
            stockRepo.save(mov);
        }

        return guardado;
    }

    @Transactional
    public TicketCocina enviarACocina(Integer idPedido) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));

        List<DetallePedido> detalles = detalleRepo.findByPedido_IdPedido(idPedido);
        List<DetallePedido> pendientes = detalles.stream()
                .filter(d -> d.getEstado() == DetallePedido.EstadoDetalle.BORRADOR)
                .toList();

        if (pendientes.isEmpty()) {
            return null;
        }

        TicketCocina ticket = new TicketCocina();
        ticket.setPedido(pedido);
        ticket.setFechaHora(LocalDateTime.now());
        ticket.setEstado(TicketCocina.EstadoTicket.IMPRESO);
        ticket.setCorrelativo(ticketRepo.findByPedido_IdPedido(idPedido).size() + 1);
        TicketCocina ticketGuardado = ticketRepo.save(ticket);

        for (DetallePedido d : pendientes) {
            d.setTicketCocina(ticketGuardado);
            d.setEstado(DetallePedido.EstadoDetalle.EN_COCINA);
            detalleRepo.save(d);
        }

        return ticketGuardado;
    }

    @Transactional
    public void eliminarItem(Integer idDetalle) {
        DetallePedido d = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new IllegalArgumentException("Detalle no encontrado: " + idDetalle));

        if (d.getEstado() != DetallePedido.EstadoDetalle.BORRADOR) {
            throw new IllegalStateException("Solo se pueden eliminar ítems en estado BORRADOR");
        }

        // Revertir stock si controla stock
        Producto prod = d.getProducto();
        if (prod != null && Boolean.TRUE.equals(prod.getControlaStock()) && prod.getStock() != null) {
            int anterior = prod.getStock();
            int nuevo = anterior + 1;
            prod.setStock(nuevo);
            productoRepo.save(prod);

            MovimientoStock mov = new MovimientoStock();
            mov.setProducto(prod);
            mov.setTipoMovimiento(MovimientoStock.TipoMovimiento.ENTRADA);
            mov.setCantidad(1);
            mov.setStockAnterior(anterior);
            mov.setStockNuevo(nuevo);
            mov.setMotivo("Eliminación de borrador en Pedido #" + d.getPedido().getIdPedido());
            mov.setFechaHora(LocalDateTime.now());
            mov.setUsuario(d.getPedido().getUsuario());
            stockRepo.save(mov);
        }

        // Eliminar cremas y salsas asociadas
        List<DetallePedidoCrema> cremas = detalleCremaRepo.findByDetallePedido_IdDetallePedido(idDetalle);
        detalleCremaRepo.deleteAll(cremas);

        List<DetallePedidoSalsa> salsas = detalleSalsaRepo.findByDetallePedido_IdDetallePedido(idDetalle);
        detalleSalsaRepo.deleteAll(salsas);

        detalleRepo.delete(d);
    }

    @Transactional
    public DetallePedido actualizarNota(Integer idDetalle, String nota) {
        DetallePedido d = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new IllegalArgumentException("Detalle no encontrado: " + idDetalle));
        d.setNota(nota);
        return detalleRepo.save(d);
    }

    @Transactional
    public DetallePedido actualizarExtras(Integer idDetalle, List<Integer> idCremas, List<Integer> idSalsas) {
        DetallePedido d = detalleRepo.findById(idDetalle)
                .orElseThrow(() -> new IllegalArgumentException("Detalle no encontrado: " + idDetalle));

        List<DetallePedidoCrema> cremasAnt = detalleCremaRepo.findByDetallePedido_IdDetallePedido(idDetalle);
        detalleCremaRepo.deleteAll(cremasAnt);

        if (idCremas != null) {
            for (Integer idCrema : idCremas) {
                cremaRepo.findById(idCrema).ifPresent(c -> {
                    DetallePedidoCrema dpc = new DetallePedidoCrema();
                    dpc.setDetallePedido(d);
                    dpc.setCrema(c);
                    detalleCremaRepo.save(dpc);
                });
            }
        }

        List<DetallePedidoSalsa> salsasAnt = detalleSalsaRepo.findByDetallePedido_IdDetallePedido(idDetalle);
        detalleSalsaRepo.deleteAll(salsasAnt);

        if (idSalsas != null) {
            for (Integer idSalsa : idSalsas) {
                productoRepo.findById(idSalsa).ifPresent(s -> {
                    DetallePedidoSalsa dps = new DetallePedidoSalsa();
                    dps.setDetallePedido(d);
                    dps.setSalsa(s);
                    detalleSalsaRepo.save(dps);
                });
            }
        }

        return d;
    }

    @Transactional
    public void cambiarEstadoDetalle(Integer idDetalle, DetallePedido.EstadoDetalle nuevoEstado) {
        detalleRepo.findById(idDetalle).ifPresent(d -> {
            d.setEstado(nuevoEstado);
            detalleRepo.save(d);
        });
    }

    @Transactional
    public void cambiarEstadoPedido(Integer idPedido, Pedido.EstadoPedido nuevoEstado) {
        pedidoRepo.findById(idPedido).ifPresent(p -> {
            p.setEstado(nuevoEstado);
            if (nuevoEstado == Pedido.EstadoPedido.PAGADA || nuevoEstado == Pedido.EstadoPedido.ANULADA) {
                p.setHoraFinal(LocalDateTime.now());
                if (p.getMesa() != null) {
                    Mesa m = p.getMesa();
                    m.setEstado(Mesa.EstadoMesa.LIBRE);
                    mesaRepo.save(m);
                }
            }
            pedidoRepo.save(p);
        });
    }

    @Transactional
    public void anularPedido(Integer idPedido, String motivo) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + idPedido));

        pedido.setEstado(Pedido.EstadoPedido.ANULADA);
        pedido.setHoraFinal(LocalDateTime.now());

        if (pedido.getMesa() != null) {
            Mesa m = pedido.getMesa();
            m.setEstado(Mesa.EstadoMesa.LIBRE);
            mesaRepo.save(m);
        }

        // Devolver stock de los detalles
        List<DetallePedido> detalles = detalleRepo.findByPedido_IdPedido(idPedido);
        for (DetallePedido d : detalles) {
            d.setEstado(DetallePedido.EstadoDetalle.ANULADO);
            detalleRepo.save(d);

            Producto prod = d.getProducto();
            if (prod != null && Boolean.TRUE.equals(prod.getControlaStock())) {
                int anterior = prod.getStock() != null ? prod.getStock() : 0;
                int nuevo = anterior + 1;
                prod.setStock(nuevo);
                productoRepo.save(prod);

                MovimientoStock mov = new MovimientoStock();
                mov.setProducto(prod);
                mov.setTipoMovimiento(MovimientoStock.TipoMovimiento.ENTRADA);
                mov.setCantidad(1);
                mov.setStockAnterior(anterior);
                mov.setStockNuevo(nuevo);
                mov.setMotivo("Anulación de Pedido #" + idPedido + ": " + motivo);
                mov.setFechaHora(LocalDateTime.now());
                mov.setUsuario(pedido.getUsuario());
                stockRepo.save(mov);
            }
        }

        if (pedido.getPedidoDelivery() != null) {
            pedido.getPedidoDelivery().setEstado(PedidoDelivery.EstadoDelivery.ANULADO);
            deliveryRepo.save(pedido.getPedidoDelivery());
        }

        pedidoRepo.save(pedido);
    }
}
