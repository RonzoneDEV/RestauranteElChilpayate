package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Incidente;
import com.cibertec.chilpayate.model.Pedido;
import com.cibertec.chilpayate.model.Usuario;
import com.cibertec.chilpayate.repository.IncidenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidenteService {

    private final IncidenteRepository incidenteRepo;

    public IncidenteService(IncidenteRepository incidenteRepo) {
        this.incidenteRepo = incidenteRepo;
    }

    @Transactional(readOnly = true)
    public List<Incidente> listar(String tipo) {
        if (tipo != null && !tipo.isBlank()) {
            return incidenteRepo.findByTipoOrderByFechaHoraDesc(tipo);
        }
        return incidenteRepo.findAllByOrderByFechaHoraDesc();
    }

    @Transactional
    public Incidente registrar(Pedido pedido, String tipo, String detalle, String motivo, Boolean devolvioStock, Usuario usuario) {
        Incidente inc = new Incidente();
        inc.setPedido(pedido);
        inc.setTipo(tipo);
        inc.setDetalle(detalle);
        inc.setMotivo(motivo);
        inc.setDevolvioStock(devolvioStock != null ? devolvioStock : false);
        inc.setFechaHora(LocalDateTime.now());
        inc.setUsuario(usuario);
        inc.setAdmin(usuario);
        return incidenteRepo.save(inc);
    }
}
