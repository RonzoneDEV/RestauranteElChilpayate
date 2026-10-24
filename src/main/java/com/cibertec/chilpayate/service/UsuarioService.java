package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Rol;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.repository.RolRepository;
import com.cibertec.chilpayate.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarioRepo, RolRepository rolRepo, PasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.encoder = encoder;
    }

    @Transactional
    public Usuario validarAcceso(String identificador, String password) {
        if (identificador == null || password == null) return null;
        String cleanUser = identificador.trim();

        // 1. Buscar por username
        Optional<Usuario> opt = usuarioRepo.findByUsernameAndEstadoTrue(cleanUser);

        // 2. O fallback al prefijo antes del @
        if (opt.isEmpty() && cleanUser.contains("@")) {
            String alias = cleanUser.substring(0, cleanUser.indexOf('@'));
            opt = usuarioRepo.findByUsernameAndEstadoTrue(alias);
        }

        if (opt.isPresent()) {
            Usuario u = opt.get();
            String stored = u.getPassword();
            if (stored != null) {
                if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
                    if (encoder.matches(password, stored)) return u;
                } else if (stored.equals(password)) {
                    // Migración al vuelo a BCrypt
                    u.setPassword(encoder.encode(password));
                    usuarioRepo.save(u);
                    return u;
                }
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarActivos() {
        return usuarioRepo.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorId(Integer id) {
        return usuarioRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorUsername(String username) {
        return usuarioRepo.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Rol> listarRoles() {
        return rolRepo.findByEstadoTrue();
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            if (!usuario.getPassword().startsWith("$2a$") && !usuario.getPassword().startsWith("$2b$")) {
                usuario.setPassword(encoder.encode(usuario.getPassword()));
            }
        }
        if (usuario.getRol() != null && usuario.getRol().getIdRol() != null) {
            rolRepo.findById(usuario.getRol().getIdRol()).ifPresent(usuario::setRol);
        }
        if (usuario.getEstado() == null) {
            usuario.setEstado(true);
        }
        return usuarioRepo.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Integer id, Usuario datos) {
        Usuario existente = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setUsername(datos.getUsername());
        if (datos.getRol() != null && datos.getRol().getIdRol() != null) {
            rolRepo.findById(datos.getRol().getIdRol()).ifPresent(existente::setRol);
        }
        if (datos.getEstado() != null) {
            existente.setEstado(datos.getEstado());
        }
        if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
            existente.setPassword(encoder.encode(datos.getPassword()));
        }
        return usuarioRepo.save(existente);
    }

    @Transactional
    public void cambiarEstado(Integer id) {
        usuarioRepo.findById(id).ifPresent(u -> {
            u.setEstado(u.getEstado() == null || !u.getEstado());
            usuarioRepo.save(u);
        });
    }
}
