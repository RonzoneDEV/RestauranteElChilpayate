package com.cibertec.chilpayate.service;

import com.cibertec.chilpayate.model.Configuracion;
import com.cibertec.chilpayate.repository.ConfiguracionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfiguracionServiceTest {

    private ConfiguracionRepository configuracionRepo;
    private ConfiguracionService configuracionService;

    @BeforeEach
    void setUp() {
        configuracionRepo = mock(ConfiguracionRepository.class);
        configuracionService = new ConfiguracionService(configuracionRepo);
    }

    @Test
    @DisplayName("Debe crear configuración por defecto con los datos solicitados si la tabla está vacía")
    void testObtenerConfiguracionPorDefecto() {
        when(configuracionRepo.findTopByOrderByIdConfiguracionAsc()).thenReturn(Optional.empty());
        when(configuracionRepo.save(any(Configuracion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Configuracion config = configuracionService.obtenerConfiguracion();

        assertNotNull(config);
        assertEquals("El Chilpayate", config.getNombre());
        assertEquals("10876343874", config.getRuc());
        assertEquals("Carlos Wiesse 625", config.getDireccion());
        assertEquals("Santo Dominguito", config.getUrbanizacion());
        assertEquals("La Libertad", config.getDepartamento());
        assertEquals("Trujillo", config.getProvincia());
        assertEquals("Trujillo", config.getDistrito());
        assertEquals(LocalTime.of(18, 0), config.getInicioTurno());
        assertEquals(LocalTime.of(23, 30), config.getFinTurno());
        assertTrue(config.getDireccionCompleta().contains("Carlos Wiesse 625"));
        assertTrue(config.getDireccionCompleta().contains("Santo Dominguito"));
        assertTrue(config.getDireccionCompleta().contains("Trujillo"));
    }

    @Test
    @DisplayName("Debe actualizar los datos de la empresa correctamente")
    void testActualizarConfiguracion() {
        Configuracion existente = new Configuracion();
        existente.setIdConfiguracion(1);
        existente.setNombre("El Chilpayate");
        existente.setRuc("10876343874");
        existente.setDireccion("Carlos Wiesse 625");
        existente.setUrbanizacion("Santo Dominguito");
        existente.setDepartamento("La Libertad");
        existente.setProvincia("Trujillo");
        existente.setDistrito("Trujillo");
        existente.setInicioTurno(LocalTime.of(18, 0));
        existente.setFinTurno(LocalTime.of(23, 30));

        when(configuracionRepo.findTopByOrderByIdConfiguracionAsc()).thenReturn(Optional.of(existente));
        when(configuracionRepo.save(any(Configuracion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Configuracion nuevosDatos = new Configuracion();
        nuevosDatos.setNombre("El Chilpayate Express");
        nuevosDatos.setRuc("20601234567");
        nuevosDatos.setDireccion("Av. Larco 1234");
        nuevosDatos.setUrbanizacion("La Merced");
        nuevosDatos.setDistrito("Víctor Larco");
        nuevosDatos.setProvincia("Trujillo");
        nuevosDatos.setDepartamento("La Libertad");
        nuevosDatos.setInicioTurno(LocalTime.of(17, 30));
        nuevosDatos.setFinTurno(LocalTime.of(23, 0));

        Configuracion resultado = configuracionService.guardar(nuevosDatos);

        assertEquals("El Chilpayate Express", resultado.getNombre());
        assertEquals("20601234567", resultado.getRuc());
        assertEquals("Av. Larco 1234", resultado.getDireccion());
        assertEquals("La Merced", resultado.getUrbanizacion());
        assertEquals(LocalTime.of(17, 30), resultado.getInicioTurno());
        assertEquals(LocalTime.of(23, 0), resultado.getFinTurno());
    }
}
