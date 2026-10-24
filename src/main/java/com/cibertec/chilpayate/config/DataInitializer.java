package com.cibertec.chilpayate.config;

import com.cibertec.chilpayate.model.*;
import com.cibertec.chilpayate.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
@Order(1)
@org.springframework.context.annotation.Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepo;
    private final UsuarioRepository usuarioRepo;
    private final CategoriaRepository categoriaRepo;
    private final MesaRepository mesaRepo;
    private final MetodoPagoRepository metodoPagoRepo;
    private final ProductoRepository productoRepo;
    private final CremaRepository cremaRepo;
    private final ClienteRepository clienteRepo;
    private final com.cibertec.chilpayate.service.ParametrosCartaService parametrosCartaService;
    private final com.cibertec.chilpayate.service.ConfiguracionService configuracionService;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public DataInitializer(RolRepository rolRepo,
                           UsuarioRepository usuarioRepo,
                           CategoriaRepository categoriaRepo,
                           MesaRepository mesaRepo,
                           MetodoPagoRepository metodoPagoRepo,
                           ProductoRepository productoRepo,
                           CremaRepository cremaRepo,
                           ClienteRepository clienteRepo,
                           com.cibertec.chilpayate.service.ParametrosCartaService parametrosCartaService,
                           com.cibertec.chilpayate.service.ConfiguracionService configuracionService,
                           org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.categoriaRepo = categoriaRepo;
        this.mesaRepo = mesaRepo;
        this.metodoPagoRepo = metodoPagoRepo;
        this.productoRepo = productoRepo;
        this.cremaRepo = cremaRepo;
        this.clienteRepo = clienteRepo;
        this.parametrosCartaService = parametrosCartaService;
        this.configuracionService = configuracionService;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        inicializarConfiguracion();
        inicializarRoles();
        inicializarCategorias();
        inicializarMesas();
        inicializarMetodosPago();
        inicializarCremas();
        inicializarClientes();
        inicializarProductos();
        inicializarParametrosCarta();
        inicializarUsuariosDemo();
    }

    private void inicializarConfiguracion() {
        configuracionService.obtenerConfiguracion();
    }

    private void inicializarRoles() {
        crearRolSiNoExiste("Administrador");
        crearRolSiNoExiste("Mozo");
        crearRolSiNoExiste("Cajero");
        crearRolSiNoExiste("Cocinero");
    }

    private Rol crearRolSiNoExiste(String nombre) {
        return rolRepo.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Rol r = new Rol();
            r.setNombre(nombre);
            r.setEstado(true);
            return rolRepo.save(r);
        });
    }

    private void inicializarCategorias() {
        crearCategoriaSiNoExiste("Alitas");
        crearCategoriaSiNoExiste("Salsas");
        crearCategoriaSiNoExiste("Salchipapas");
        crearCategoriaSiNoExiste("Hamburguesas");
        crearCategoriaSiNoExiste("Broaster");
        crearCategoriaSiNoExiste("Bebidas");
        crearCategoriaSiNoExiste("Adicionales");
    }

    private void crearCategoriaSiNoExiste(String nombre) {
        if (!categoriaRepo.existsByNombreIgnoreCase(nombre)) {
            Categoria c = new Categoria();
            c.setNombre(nombre);
            c.setEstado(true);
            categoriaRepo.save(c);
        }
    }

    private void inicializarMesas() {
        if (mesaRepo.count() == 0) {
            for (int i = 1; i <= 8; i++) {
                Mesa m = new Mesa();
                m.setNumero(i);
                m.setCapacidad(i % 2 == 0 ? 4 : 2);
                m.setSalon("Salón Principal");
                m.setEstado(Mesa.EstadoMesa.LIBRE);
                m.setActivo(true);
                mesaRepo.save(m);
            }
        }
    }

    private void inicializarMetodosPago() {
        if (metodoPagoRepo.count() == 0) {
            List<String> metodos = List.of("EFECTIVO", "YAPE", "PLIN","TRANSFERENCIA");
            for (String m : metodos) {
                MetodoPago mp = new MetodoPago();
                mp.setNombre(m);
                mp.setEstado(true);
                metodoPagoRepo.save(mp);
            }
        }
    }

    private void inicializarUsuariosDemo() {
        Rol rolAdmin = rolRepo.findByNombreIgnoreCase("Administrador").orElse(null);
        Rol rolMozo = rolRepo.findByNombreIgnoreCase("Mozo").orElse(null);
        Rol rolCajero = rolRepo.findByNombreIgnoreCase("Cajero").orElse(null);
        Rol rolCocinero = rolRepo.findByNombreIgnoreCase("Cocinero").orElse(null);


        if (usuarioRepo.findByUsername("admin").isEmpty() && rolAdmin != null) {
            Usuario u = new Usuario();
            u.setUsername("admin");
            u.setPassword(encoder.encode("admin123"));
            u.setNombres("Administrador");
            u.setApellidos("General");
            u.setRol(rolAdmin);
            u.setEstado(true);
            usuarioRepo.save(u);
        }

        if (usuarioRepo.findByUsername("mozo").isEmpty() && rolMozo != null) {
            Usuario u = new Usuario();
            u.setUsername("mozo");
            u.setPassword(encoder.encode("mozo123"));
            u.setNombres("Carlos");
            u.setApellidos("Mozo");
            u.setRol(rolMozo);
            u.setEstado(true);
            usuarioRepo.save(u);
        }

        // Usuario Cajero demo
        if (usuarioRepo.findByUsername("cajero").isEmpty() && usuarioRepo.findByUsername("caja").isEmpty() && rolCajero != null) {
            Usuario u = new Usuario();
            u.setUsername("caja");
            u.setPassword(encoder.encode("caja123"));
            u.setNombres("María");
            u.setApellidos("Cajera");
            u.setRol(rolCajero);
            u.setEstado(true);
            usuarioRepo.save(u);
        }

        // Usuario Cocinero demo
        if (usuarioRepo.findByUsername("cocina").isEmpty() && rolCocinero != null) {
            Usuario u = new Usuario();
            u.setUsername("cocina");
            u.setPassword(encoder.encode("cocina123"));
            u.setNombres("Juan");
            u.setApellidos("Cocinero");
            u.setRol(rolCocinero);
            u.setEstado(true);
            usuarioRepo.save(u);
        }
    }

    private void inicializarCremas() {
        if (cremaRepo.count() == 0) {
            List<String> cremas = List.of("Mayonesa Ahumada", "Tártara de la Casa", "Ají Parrillero", "Salsa Golf", "Crema de Rocoto");
            for (String c : cremas) {
                Crema cr = new Crema();
                cr.setNombre(c);
                cr.setEstado(true);
                cremaRepo.save(cr);
            }
        }
    }

    private void inicializarClientes() {
        crearClienteSiNoExiste("Juan Carlos Quispe Morales", "987654321", "Av. Brasil 2450");
        crearClienteSiNoExiste("María Elena Rodríguez Vargas", "991234567", "Jr. San Martín 380");
        crearClienteSiNoExiste("Carlos Alberto Mendoza Peña", "976543210", "Av. La Marina 1520 Dpto. 402");
        crearClienteSiNoExiste("Ana Lucía Benavides Flores", "984321987", "Calle Las Begonias 742");
        crearClienteSiNoExiste("Jorge Luis Huamán Ramos", "998877665", "Av. Arequipa 3210");
        crearClienteSiNoExiste("Sofía Valeria Chávez Castro", "971122334", "Jr. Bolognesi 185");
        crearClienteSiNoExiste("Diego Fernando Silva Alarcón", "983456789", "Av. Universitaria 1980");
        crearClienteSiNoExiste("Patricia Roxana Gutiérrez Díaz", "992345678", "Av. Colonial 540, Bellavista");
        crearClienteSiNoExiste("Gonzalo Javier Paredes Ríos", "965432198", "Calle Ramón Zavala 312");
        crearClienteSiNoExiste("Valeria Nicole Córdova Romero", "978901234", "Av. Sucre 875");
    }

    private void crearClienteSiNoExiste(String nombre, String telefono, String direccion) {
        if (!clienteRepo.existsByTelefono(telefono)) {
            Cliente c = new Cliente();
            c.setNombre(nombre);
            c.setTelefono(telefono);
            c.setDireccion(direccion);
            c.setEstado(true);
            clienteRepo.save(c);
        }
    }

    private void inicializarProductos() {
        Categoria catAlitas = categoriaRepo.findByNombreIgnoreCase("Alitas").orElse(null);
        Categoria catSalchi = categoriaRepo.findByNombreIgnoreCase("Salchipapas").orElse(null);
        Categoria catHamburguesas = categoriaRepo.findByNombreIgnoreCase("Hamburguesas").orElse(null);
        Categoria catBroaster = categoriaRepo.findByNombreIgnoreCase("Broaster").orElse(null);
        Categoria catBebidas = categoriaRepo.findByNombreIgnoreCase("Bebidas").orElse(null);
        Categoria catAdicionales = categoriaRepo.findByNombreIgnoreCase("Adicionales").orElse(null);
        Categoria catSalsa = categoriaRepo.findByNombreIgnoreCase("Salsas")
                .orElseGet(() -> categoriaRepo.findByNombreIgnoreCase("Salsa").orElse(null));


        if (catAlitas != null) {
            crearProductoSiNoExiste("Alitas Combo x6", "Alitas de 6 porciones", new BigDecimal("18.00"), catAlitas, true, 50, true, true, 1);
            crearProductoSiNoExiste("Alitas Combo x 12", "Alitas de 12 porciones", new BigDecimal("34.00"), catAlitas, true, 50, true, true, 2);
            crearProductoSiNoExiste("Alitas Combo x 20", "Alitas de 20 porciones", new BigDecimal("50.00"), catAlitas, true, 40, true, true, 4);
            crearProductoSiNoExiste("Alitas Combo x 50", "Alitas de 50 porciones", new BigDecimal("120.00"), catAlitas, true, 30, true, true, 6);
        }


        if (catHamburguesas != null) {
            crearProductoSiNoExiste("Hamburguesa Clásica Chilpayate", "Carne artesanal 180g, lechuga, tomate y papas al hilo crocantes", new BigDecimal("15.00"), catHamburguesas, true, 40, true, false, 0);
            crearProductoSiNoExiste("Hamburguesa Royal Especial", "Carne artesanal 180g, queso cheddar fundido, huevo montado y papas nativas", new BigDecimal("18.00"), catHamburguesas, true, 40, true, false, 0);
            crearProductoSiNoExiste("Hamburguesa A lo Pobre", "Doble carne artesanal, plátano frito, huevo montado, queso y papas fritas", new BigDecimal("22.00"), catHamburguesas, true, 35, true, false, 0);
            crearProductoSiNoExiste("Hamburguesa BBQ Tocino", "Carne artesanal con tocino crujiente, queso cheddar y salsa BBQ", new BigDecimal("20.00"), catHamburguesas, true, 35, true, false, 0);
            crearProductoSiNoExiste("Hamburguesa Crispy Chicken", "Pechuga de pollo crocante empanizada estilo broaster, lechuga y mayonesa", new BigDecimal("17.00"), catHamburguesas, true, 40, true, false, 0);
        }

        if (catBroaster != null) {
            crearProductoSiNoExiste("1/4 Pollo Broaster Clásico", "Pierna o pechuga crocante acompañada de papas fritas y ensalada fresca", new BigDecimal("16.00"), catBroaster, true, 50, true, false, 0);
            crearProductoSiNoExiste("1/2 Pollo Broaster Familiar", "Dos piezas grandes de pollo broaster con doble porción de papas y ensalada", new BigDecimal("30.00"), catBroaster, true, 30, true, false, 0);
            crearProductoSiNoExiste("Mostrito Broaster", "1/4 de pollo broaster con arroz chaufa al wok y papas fritas", new BigDecimal("19.00"), catBroaster, true, 45, true, false, 0);
            crearProductoSiNoExiste("Nuggets de Pollo Crocantes x10", "10 nuggets de pollo artesanales dorados con papas fritas", new BigDecimal("14.00"), catBroaster, true, 40, true, false, 0);
        }

        if (catSalchi != null) {
            crearProductoSiNoExiste("Salchipapa Chilpayate", "Papas nativas fritas, hot dog frankfurter y huevo montado", new BigDecimal("22.00"), catSalchi, true, 40, true, false, 0);
            crearProductoSiNoExiste("Salchipapa Clásica", "Papas fritas crocantes con abundante hot dog frankfurter en rodajas", new BigDecimal("14.00"), catSalchi, true, 45, true, false, 0);
            crearProductoSiNoExiste("Salchipapa Mixta Especial", "Papas nativas, hot dog frankfurter, chorizo parrillero y queso fundido", new BigDecimal("24.00"), catSalchi, true, 35, true, false, 0);
            crearProductoSiNoExiste("Salchitocino Extrema", "Papas amarillas, hot dog, trozos generosos de tocino crocante y huevo frito", new BigDecimal("25.00"), catSalchi, true, 30, true, false, 0);
            crearProductoSiNoExiste("Salchipollo", "Papas fritas con hot dog y jugosas tiras de pechuga de pollo a la plancha", new BigDecimal("22.00"), catSalchi, true, 35, true, false, 0);
        }

        if (catBebidas != null) {
            crearProductoSiNoExiste("Chicha Morada Casera 1L", "Preparada con maíz morado, canela, clavo de olor y piña", new BigDecimal("12.00"), catBebidas, true, 60, false, false, 0);
            crearProductoSiNoExiste("Gaseosa Inka Kola 500ml", "Gaseosa personal bien helada", new BigDecimal("5.00"), catBebidas, true, 80, false, false, 0);
            crearProductoSiNoExiste("Gaseosa Coca Cola 500ml", "Gaseosa personal bien helada", new BigDecimal("5.00"), catBebidas, true, 80, false, false, 0);
            crearProductoSiNoExiste("Gaseosa Inka Kola 1.5L", "Botella mediana descartable para compartir", new BigDecimal("10.00"), catBebidas, true, 40, false, false, 0);
            crearProductoSiNoExiste("Gaseosa Coca Cola 1.5L", "Botella mediana descartable para compartir", new BigDecimal("10.00"), catBebidas, true, 40, false, false, 0);
            crearProductoSiNoExiste("Limonada Frozen 1L", "Jarra de limonada natural frappeada al instante", new BigDecimal("12.00"), catBebidas, true, 50, false, false, 0);
            crearProductoSiNoExiste("Maracuyá Natural 1L", "Jarra de jugo natural refrescante de maracuyá", new BigDecimal("12.00"), catBebidas, true, 50, false, false, 0);
            crearProductoSiNoExiste("Agua Mineral San Luis 500ml", "Agua mineral de mesa sin gas", new BigDecimal("3.00"), catBebidas, true, 60, false, false, 0);
        }

        if (catAdicionales != null) {
            crearProductoSiNoExiste("Porción de Papas Fritas", "Papas amarillas crujientes aromatizadas con sal y orégano", new BigDecimal("8.00"), catAdicionales, true, 50, true, false, 0);
            crearProductoSiNoExiste("Porción de Tequeños de Queso x8", "Tequeños rellenos de queso con crema de palta guacamole", new BigDecimal("14.00"), catAdicionales, true, 40, true, false, 0);
            crearProductoSiNoExiste("Porción de Arroz Chaufa", "Arroz chaufa al wok con cebollita china y huevo", new BigDecimal("7.00"), catAdicionales, true, 40, true, false, 0);
            crearProductoSiNoExiste("Porción de Ensalada Fresca", "Lechuga, rodajas de tomate, pepino y vinagreta artesanal", new BigDecimal("5.00"), catAdicionales, true, 40, false, false, 0);
            crearProductoSiNoExiste("Huevo Frito Montado", "1 unidad montada al momento", new BigDecimal("2.50"), catAdicionales, true, 60, false, false, 0);
            crearProductoSiNoExiste("Porción de Queso Cheddar Fundido", "Extra de queso cheddar líquido caliente", new BigDecimal("4.00"), catAdicionales, true, 50, false, false, 0);
            crearProductoSiNoExiste("Porción de Plátano Frito", "2 unidades de plátano maduro frito", new BigDecimal("3.00"), catAdicionales, true, 40, false, false, 0);
        }

        if (catSalsa != null && productoRepo.findByCategoria_IdCategoriaAndEstadoTrue(catSalsa.getIdCategoria()).isEmpty()) {
            crearProductoSiNoExiste("BBQ", "Salsa casera ahumada adicional", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("BBQ Picante", "Salsa casera con picante adicional", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Acevichada", "Salsa acevichada de la casa", new BigDecimal("3.50"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Honey Mustard", "Salsa de aderezo agridulce y cremoso", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Tocino", "Salsa cremosa con trozos crujientes de tocino ahumado", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Uchucuta Picante", "Tradicional crema andina a base de ajíes y rocoto", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Olivo", "Emulsión suave y aterciopelada a base de aceitunas botija", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Al Ajo Anticuchera", "Fusión criolla intensa con ají panca y ajo dorado", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
            crearProductoSiNoExiste("Maracuya", "Salsa agridulce y brillante a base de pulpa natural de maracuyá", new BigDecimal("3.00"), catSalsa, false, 0, false, true, 0);
        }
    }

    private void crearProductoSiNoExiste(String nombre, String desc, BigDecimal precio, Categoria cat, boolean controlaStock, int stock, boolean permiteCrema, boolean permiteSalsa, int limiteSalsa) {
        if (cat == null) return;
        if (!productoRepo.existsByNombreIgnoreCase(nombre)) {
            Producto p = new Producto();
            p.setNombre(nombre);
            p.setDescripcion(desc);
            p.setPrecio(precio);
            p.setCategoria(cat);
            p.setControlaStock(controlaStock);
            p.setStock(stock);
            p.setPermiteCrema(permiteCrema);
            p.setPermiteSalsa(permiteSalsa);
            p.setLimiteSalsa(limiteSalsa);
            p.setTipo(p.inferirTipo());
            p.setEstado(true);
            productoRepo.save(p);
        }
    }

    private void inicializarParametrosCarta() {
        parametrosCartaService.obtenerParametros();

        List<Producto> productos = productoRepo.findAll();
        for (Producto prod : productos) {
            boolean esSalsa = (prod.getCategoria() != null &&
                    (prod.getCategoria().getNombre().equalsIgnoreCase("SALSA") ||
                     prod.getCategoria().getNombre().equalsIgnoreCase("SALSAS")))
                    || (prod.getNombre() != null && prod.getNombre().toLowerCase().contains("salsa"));
            if (esSalsa && prod.getTipo() != TipoProducto.EXTRA) {
                prod.setTipo(TipoProducto.EXTRA);
                productoRepo.save(prod);
            }
        }
    }
}
