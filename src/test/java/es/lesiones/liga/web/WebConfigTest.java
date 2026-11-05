package es.lesiones.liga.web;

import es.lesiones.liga.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/** En producción la app no debe arrancar sin un ADMIN_TOKEN propio. */
class WebConfigTest {

    private static WebConfig configuracion(String token, String... perfiles) {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles(perfiles);
        return new WebConfig(null, new AppProperties(token, 5), entorno);
    }

    @Test
    void enProduccionRechazaElTokenPorDefecto() {
        assertThrows(IllegalStateException.class, () -> configuracion("cambia-esto", "prod").exigirTokenEnProduccion());
    }

    @Test
    void enProduccionRechazaTokenVacioONulo() {
        assertThrows(IllegalStateException.class, () -> configuracion("", "prod").exigirTokenEnProduccion());
        assertThrows(IllegalStateException.class, () -> configuracion(null, "prod").exigirTokenEnProduccion());
    }

    @Test
    void enProduccionAceptaUnTokenPropio() {
        assertDoesNotThrow(() -> configuracion("una-clave-propia-y-larga-123", "prod").exigirTokenEnProduccion());
    }

    @Test
    void fueraDeProduccionPermiteElTokenPorDefecto() {
        assertDoesNotThrow(() -> configuracion("cambia-esto").exigirTokenEnProduccion());
        assertDoesNotThrow(() -> configuracion("cambia-esto", "demo").exigirTokenEnProduccion());
    }
}
