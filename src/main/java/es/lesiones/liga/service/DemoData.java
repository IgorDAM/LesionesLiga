package es.lesiones.liga.service;

import es.lesiones.liga.domain.*;
import es.lesiones.liga.domain.Episodio.Lado;
import es.lesiones.liga.repo.*;
import es.lesiones.liga.util.Fechas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos FICTICIOS para probar la web sin tener nada real cargado (perfil "demo").
 * Se cuelgan de un club inventado para que nunca se confundan con jugadores reales.
 */
@Slf4j
@Component
@Profile("demo")
@Order(2)
@RequiredArgsConstructor
public class DemoData implements CommandLineRunner {

    private static final String CLUB = "Club Demo (datos ficticios)";
    private static final String URL = "https://example.com/demo";

    private final ClubRepository clubes;
    private final JugadorRepository jugadores;
    private final TipoRepository tipos;
    private final EpisodioRepository episodios;

    @Override
    @Transactional
    public void run(String... args) {
        if (clubes.findByNombreIgnoreCase(CLUB).isPresent()) {
            return;
        }
        Club club = new Club();
        club.setNombre(CLUB);
        club.setDivision(Club.Division.PRIMERA);
        club = clubes.save(club);

        List<Jugador> plantilla = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Jugador j = new Jugador();
            j.setNombre("Jugador Demo " + String.format("%02d", i));
            j.setClub(club);
            j.setPosicion(Jugador.Posicion.values()[i % 4]);
            plantilla.add(jugadores.save(j));
        }

        int n = 0;
        // Isquiotibial: 6 roturas cerradas (muestra suficiente) + 1 activa
        for (int dias : new int[]{21, 28, 35, 24, 42, 30}) {
            caso(plantilla.get(n % 10), "isquiotibial", "Rotura fibrilar", Lado.DERECHO, dias, 28, false, false, n++);
        }
        caso(plantilla.get(3), "isquiotibial", "Rotura fibrilar", Lado.IZQUIERDO, null, 28, false, false, n++);
        // Rodilla: 3 LCA cerrados (muestra pequeña: la web debe avisar) + 1 activo
        for (int dias : new int[]{215, 240, 268}) {
            caso(plantilla.get(n % 10), "rodilla", "Rotura de ligamento cruzado anterior", Lado.DERECHO, dias, 240, true, false, n++);
        }
        caso(plantilla.get(5), "rodilla", "Rotura de ligamento cruzado anterior", Lado.IZQUIERDO, null, 240, true, false, n++);
        // Tobillo: 4 esguinces cerrados
        for (int dias : new int[]{18, 25, 21, 30}) {
            caso(plantilla.get(n % 10), "tobillo", "Esguince grado II-III", Lado.IZQUIERDO, dias, 21, false, false, n++);
        }
        log.info("Datos de demostración cargados ({} episodios ficticios)", n);
    }

    /** dias == null -> lesión activa (empezó hace 9 días). */
    private void caso(Jugador j, String zona, String nombreTipo, Lado lado, Integer dias,
                      int estimado, boolean cirugia, boolean recaida, int orden) {
        TipoLesion tipo = tipos.findByZonaCodigoOrderByNombreAsc(zona).stream()
                .filter(t -> t.getNombre().equals(nombreTipo)).findFirst().orElseThrow();
        LocalDate hoy = Fechas.hoy();
        Episodio e = new Episodio();
        e.setJugador(j);
        e.setTipo(tipo);
        e.setLado(lado);
        if (dias == null) {
            e.setFechaInicio(hoy.minusDays(9));
        } else {
            e.setFechaInicio(hoy.minusDays(dias + 20L + orden * 12L));
            e.setFechaAlta(e.getFechaInicio().plusDays(dias));
            e.setPartidosPerdidos(Math.max(1, dias / 7));
        }
        e.setCirugia(cirugia);
        e.setRecaida(recaida);
        e.setPlazoEstimadoDias(estimado);
        e.setDiagnosticoClub("DATOS FICTICIOS DE DEMOSTRACIÓN");
        e.setFuenteUrl(URL);
        e.setFuenteNombre("Demo");
        e.setNotas("DATOS FICTICIOS DE DEMOSTRACIÓN");
        episodios.save(e);
    }
}
