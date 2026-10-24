package com.cibertec.chilpayate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.repository.UsuarioRepository;


@Component
public class MigracionClaves implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionClaves.class);

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public MigracionClaves(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    private boolean esHash(String c) {
        return c != null && (c.startsWith("$2a$") || c.startsWith("$2b$") || c.startsWith("$2y$"));
    }

    @Override
    public void run(String... args) {
        int n = 0;
        for (Usuario u : repo.findAll()) {
            if (u.getPassword() != null && !esHash(u.getPassword())) {
                u.setPassword(encoder.encode(u.getPassword()));
                repo.save(u);
                n++;
            }
        }
        if (n > 0) {
            log.info("[SEGURIDAD] Contraseñas migradas a BCrypt: {}", n);
        }
    }
}
