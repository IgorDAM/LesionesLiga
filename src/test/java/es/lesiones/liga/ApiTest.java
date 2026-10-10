package es.lesiones.liga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.lesiones.liga.util.Fechas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiTest {

    private static final String TOKEN = "token-de-prueba";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    // ---------- Utilidades ----------

    private JsonNode consultar(String url) throws Exception {
        return om.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder b, Object cuerpo) throws Exception {
        b = b.header("X-Admin-Token", TOKEN);
        return cuerpo == null ? b : b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(cuerpo));
    }

    private JsonNode admin(MockHttpServletRequestBuilder b, Object cuerpo, int estadoEsperado) throws Exception {
        var res = mvc.perform(conToken(b, cuerpo)).andReturn().getResponse();
        assertEquals(estadoEsperado, res.getStatus(), "Respuesta: " + res.getContentAsString());
        return res.getContentAsString().isBlank() ? null : om.readTree(res.getContentAsString());
    }

    private long tipoId(String zona, String nombre) throws Exception {
        for (JsonNode t : consultar("/api/tipos?zona=" + zona)) {
            if (t.get("nombre").asText().equals(nombre)) {
                return t.get("id").asLong();
            }
        }
        throw new AssertionError("No existe el tipo " + zona + "/" + nombre);
    }

    private long nuevoJugador(String nombre) throws Exception {
        long clubId = consultar("/api/clubes").get(0).get("id").asLong();
        Map<String, Object> j = new LinkedHashMap<>();
        j.put("nombre", nombre);
        j.put("clubId", clubId);
        j.put("posicion", "DEFENSA");
        return admin(post("/api/admin/jugadores"), j, 201).get("id").asLong();
    }

    private Map<String, Object> episodio(long jugadorId, long tipoId, String lado, LocalDate inicio, LocalDate alta) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("jugadorId", jugadorId);
        e.put("tipoId", tipoId);
        e.put("lado", lado);
        e.put("fechaInicio", inicio.toString());
        if (alta != null) {
            e.put("fechaAlta", alta.toString());
        }
        e.put("fuenteUrl", "https://example.com/parte");
        return e;
    }

    // ---------- Tests ----------

    @Test
    void catalogoYClubesSeCarganAlArrancar() throws Exception {
        assertEquals(22, consultar("/api/zonas").size());
        assertTrue(consultar("/api/tipos").size() >= 60);
        JsonNode clubes = consultar("/api/clubes");
        assertEquals(42, clubes.size());
    }

    @Test
    void laAdministracionExigeToken() throws Exception {
        mvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/ping").header("X-Admin-Token", "incorrecto")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/ping").header("X-Admin-Token", TOKEN)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/jugadores").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laBusquedaIgnoraTildesYMayusculas() throws Exception {
        nuevoJugador("Íñigo Martínez Prueba");
        assertEquals(1, consultar("/api/jugadores?q=inigo martinez").size());
        assertEquals(1, consultar("/api/jugadores?q=IÑIGO").size());
        assertEquals(0, consultar("/api/jugadores?q=i").size(), "menos de 2 letras no busca");
    }

    @Test
    void estadisticasDeRecuperacionYFicha() throws Exception {
        long tipo = tipoId("isquiotibial", "Rotura fibrilar");
        long jugador = nuevoJugador("Jugador Estadistica");
        LocalDate hoy = Fechas.hoy();

        // Tres casos cerrados de 20, 30 y 40 días -> media 30, mediana 30
        int[] duraciones = {20, 30, 40};
        for (int i = 0; i < duraciones.length; i++) {
            LocalDate inicio = hoy.minusDays(300 - i * 60L);
            admin(post("/api/admin/episodios"), episodio(jugador, tipo, "DERECHO", inicio, inicio.plusDays(duraciones[i])), 201);
        }
        // Un caso activo
        admin(post("/api/admin/episodios"), episodio(jugador, tipo, "IZQUIERDO", hoy.minusDays(5), null), 201);

        JsonNode est = consultar("/api/estadisticas/tipo/" + tipo);
        assertEquals(3, est.get("casosCerrados").asInt());
        assertEquals(1, est.get("casosActivos").asInt());
        assertEquals(30.0, est.get("medianaDias").asDouble(), 0.001);
        assertEquals(30.0, est.get("mediaDias").asDouble(), 0.001);
        assertEquals(20, est.get("minDias").asLong());
        assertEquals(40, est.get("maxDias").asLong());
        assertTrue(est.get("pocosCasos").asBoolean(), "3 casos < mínimo de 5");

        JsonNode ficha = consultar("/api/jugadores/" + jugador);
        assertEquals(1, ficha.get("activas").size());
        assertEquals(3, ficha.get("historial").size());
        assertEquals(5, ficha.get("activas").get(0).get("diasBaja").asLong());

        boolean aparece = false;
        for (JsonNode a : consultar("/api/lesiones/activas?division=PRIMERA")) {
            aparece |= a.get("jugadorId").asLong() == jugador;
        }
        assertTrue(aparece);
        assertEquals("IZQUIERDO", consultar("/api/lesiones/activas?zona=isquiotibial").get(0).get("lado").asText());
    }

    @Test
    void validacionesDeEpisodio() throws Exception {
        long jugador = nuevoJugador("Jugador Validaciones");
        long tipoBilateral = tipoId("rodilla", "Lesión de menisco");
        long tipoCentral = tipoId("pubis", "Pubalgia");
        LocalDate hoy = Fechas.hoy();

        // Zona bilateral sin lado -> 400
        admin(post("/api/admin/episodios"), episodio(jugador, tipoBilateral, "NO_APLICA", hoy.minusDays(3), null), 400);
        // Fecha de inicio futura -> 400
        admin(post("/api/admin/episodios"), episodio(jugador, tipoBilateral, "DERECHO", hoy.plusDays(2), null), 400);
        // Alta anterior al inicio -> 400
        admin(post("/api/admin/episodios"), episodio(jugador, tipoBilateral, "DERECHO", hoy.minusDays(3), hoy.minusDays(10)), 400);
        // Fuente que no es una URL -> 400
        Map<String, Object> sinUrl = episodio(jugador, tipoBilateral, "DERECHO", hoy.minusDays(3), null);
        sinUrl.put("fuenteUrl", "no-es-una-url");
        admin(post("/api/admin/episodios"), sinUrl, 400);

        // Zona no bilateral: el lado se fuerza a NO_APLICA aunque se envíe otro
        JsonNode central = admin(post("/api/admin/episodios"), episodio(jugador, tipoCentral, "DERECHO", hoy.minusDays(3), null), 201);
        assertEquals("NO_APLICA", central.get("lado").asText());
        // Misma lesión abierta dos veces -> 409
        admin(post("/api/admin/episodios"), episodio(jugador, tipoCentral, "NO_APLICA", hoy.minusDays(1), null), 409);

        // Dar de alta y comprobar que deja de estar activa
        long id = central.get("id").asLong();
        Map<String, Object> alta = Map.of("fechaAlta", hoy.toString(), "partidosPerdidos", 2);
        JsonNode cerrado = admin(patch("/api/admin/episodios/" + id + "/alta"), alta, 200);
        assertFalse(cerrado.get("activa").asBoolean());
        assertEquals(3, cerrado.get("diasBaja").asLong());
    }

    @Test
    void exportacionCsvIncluyeCabeceraYNeutralizaFormulas() throws Exception {
        long jugador = nuevoJugador("=CMD Jugador Formula");
        long tipo = tipoId("pubis", "Osteítis de pubis");
        admin(post("/api/admin/episodios"), episodio(jugador, tipo, "NO_APLICA", Fechas.hoy().minusDays(2), null), 201);

        String csv = mvc.perform(conToken(get("/api/admin/export/episodios.csv"), null))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(csv.contains("id;jugador;club;division"));
        assertTrue(csv.contains("\"'=CMD Jugador Formula\""), "los textos que empiezan por = deben neutralizarse");
    }
}
