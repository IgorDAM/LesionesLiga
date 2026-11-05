package es.lesiones.liga.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba unitaria pura (sin Spring ni base de datos) del cálculo de percentiles. */
class PercentilTest {

    @Test
    void listaVaciaNoTienePercentil() {
        assertNull(ConsultaService.percentil(List.of(), 0.5));
    }

    @Test
    void conUnSoloValorTodosLosPercentilesLoDevuelven() {
        assertEquals(42.0, ConsultaService.percentil(List.of(42L), 0.25), 0.0001);
        assertEquals(42.0, ConsultaService.percentil(List.of(42L), 0.75), 0.0001);
    }

    @Test
    void interpolaEntreValoresVecinos() {
        List<Long> dias = List.of(10L, 20L, 30L, 40L);
        assertEquals(10.0, ConsultaService.percentil(dias, 0), 0.0001);
        assertEquals(17.5, ConsultaService.percentil(dias, 0.25), 0.0001);
        assertEquals(25.0, ConsultaService.percentil(dias, 0.5), 0.0001);
        assertEquals(32.5, ConsultaService.percentil(dias, 0.75), 0.0001);
        assertEquals(40.0, ConsultaService.percentil(dias, 1), 0.0001);
    }

    @Test
    void conCantidadImparCoincideConLosValoresExactos() {
        List<Long> dias = List.of(10L, 20L, 30L, 40L, 50L);
        assertEquals(20.0, ConsultaService.percentil(dias, 0.25), 0.0001);
        assertEquals(30.0, ConsultaService.percentil(dias, 0.5), 0.0001);
        assertEquals(40.0, ConsultaService.percentil(dias, 0.75), 0.0001);
    }
}
