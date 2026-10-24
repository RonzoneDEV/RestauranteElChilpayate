package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Cliente;
import com.cibertec.chilpayate.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepo;

    public ClienteService(ClienteRepository clienteRepo) {
        this.clienteRepo = clienteRepo;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarActivos() {
        return clienteRepo.findByEstadoTrue();
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscar(String q) {
        if (q == null || q.isBlank()) return listarActivos();
        return clienteRepo.findByNombreContainingIgnoreCaseOrTelefonoContaining(q, q);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> obtenerPorId(Integer id) {
        return clienteRepo.findById(id);
    }

    @Transactional
    public Cliente guardar(Cliente c) {
        if (c.getNombre() == null || c.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        if (c.getTelefono() == null || c.getTelefono().isBlank()) {
            c.setTelefono("-");
        }
        if (c.getDireccion() == null) {
            c.setDireccion("");
        }
        if (c.getEstado() == null) c.setEstado(true);
        return clienteRepo.save(c);
    }

    @Transactional
    public Cliente actualizar(Integer id, Cliente c) {
        Cliente existente = clienteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
        existente.setNombre(c.getNombre());
        existente.setTelefono(c.getTelefono() != null && !c.getTelefono().isBlank() ? c.getTelefono() : "-");
        existente.setDireccion(c.getDireccion() != null ? c.getDireccion() : "");
        if (c.getEstado() != null) {
            existente.setEstado(c.getEstado());
        }
        return clienteRepo.save(existente);
    }

    @Transactional
    public void cambiarEstado(Integer id) {
        clienteRepo.findById(id).ifPresent(c -> {
            c.setEstado(c.getEstado() == null || !c.getEstado());
            clienteRepo.save(c);
        });
    }
}
