package es.lesiones.liga.service;

import es.lesiones.liga.domain.Club;
import es.lesiones.liga.domain.Episodio;
import es.lesiones.liga.domain.Jugador;
import es.lesiones.liga.domain.TipoLesion;
import es.lesiones.liga.repo.ClubRepository;
import es.lesiones.liga.repo.EpisodioRepository;
import es.lesiones.liga.repo.JugadorRepository;
import es.lesiones.liga.repo.TipoRepository;
import es.lesiones.liga.util.Fechas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga seed/lesiones.csv (lesiones recopiladas de prensa/webs). La columna opcional 13 «fecha_alta» marca las ya
 * recuperadas: se guardan con fecha de baja y de alta (histórico para comparar tiempos de recuperación). Idempotente:
 * si el jugador ya tiene una lesión abierta del mismo tipo y lado no se duplica (y, si la fila trae alta, se cierra);
 * un episodio con el mismo inicio nunca se vuelve a crear ni a abrir. No se ejecuta en los perfiles demo ni test.
 * Los episodios cargados quedan marcados en «notas» con su origen y, si procede, con REVISAR.
 */
@Slf4j
@Component
@Order(2)
@Profile("!demo & !test")
@RequiredArgsConstructor
public class LesionesLoader implements CommandLineRunner {

    private final ClubRepository clubes;
    private final JugadorRepository jugadores;
    private final TipoRepository tipos;
    private final EpisodioRepository episodios;

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        int nuevos = 0, existentes = 0, omitidos = 0, cerrados = 0;
        for (String[] f : leer("seed/lesiones.csv")) {
            if (f.length < 10) { omitidos++; continue; }
            Club club = clubes.findByNombreIgnoreCase(f[0]).orElse(null);
            TipoLesion tipo = tipos.findByZonaCodigoOrderByNombreAsc(f[2]).stream()
                    .filter(t -> t.getNombre().equalsIgnoreCase(f[3])).findFirst().orElse(null);
            if (club == null || tipo == null) {
                log.warn("lesiones.csv: club o tipo desconocido ({} / {} {}), línea omitida", f[0], f[2], f[3]);
                omitidos++;
                continue;
            }
            Jugador jugador = jugadores.findAll().stream()
                    .filter(j -> j.getClub().getId().equals(club.getId()) && j.getNombre().equalsIgnoreCase(f[1]))
                    .findFirst().orElseGet(() -> {
                        Jugador nuevo = new Jugador();
                        nuevo.setNombre(f[1]);
                        nuevo.setClub(club);
                        return jugadores.save(nuevo);
                    });
            Episodio.Lado lado = Episodio.Lado.valueOf(f[4]);
            LocalDate inicio = LocalDate.parse(f[5]);
            LocalDate alta = f.length > 12 && !f[12].isBlank() ? LocalDate.parse(f[12].trim()) : null;
            if (alta != null && alta.isBefore(inicio)) {
                log.warn("lesiones.csv: fecha_alta anterior al inicio ({} {}), línea omitida", f[1], f[3]);
                omitidos++;
                continue;
            }
            List<Episodio> abiertos = episodios.findByJugadorIdAndTipoIdAndLadoAndFechaAltaIsNull(
                    jugador.getId(), tipo.getId(), lado);
            if (!abiertos.isEmpty()) {
                if (alta != null) {
                    // La lesión ya está recuperada: se cierra el episodio abierto para conservar el histórico.
                    for (Episodio abierto : abiertos) {
                        abierto.setFechaAlta(alta);
                        abierto.setNotas((abierto.getNotas() == null ? "" : abierto.getNotas() + " ")
                                + "Alta registrada el " + alta + " (carga de seed).");
                        episodios.save(abierto);
                    }
                    cerrados++;
                } else {
                    existentes++;
                }
                continue;
            }
            // Mismo jugador, tipo, lado y fecha de inicio ya registrados (p. ej. episodio ya cerrado): no se duplica ni se reabre.
            if (episodios.existsByJugadorIdAndTipoIdAndLadoAndFechaInicio(jugador.getId(), tipo.getId(), lado, inicio)) {
                existentes++;
                continue;
            }
            Episodio e = new Episodio();
            e.setJugador(jugador);
            e.setTipo(tipo);
            e.setLado(lado);
            e.setFechaInicio(inicio);
            e.setFechaAlta(alta);
            e.setCirugia(Boolean.parseBoolean(f[6]));
            e.setDiagnosticoClub(f[8]);
            e.setFuenteUrl(f[9]);
            e.setFuenteNombre(nombreFuente(f[9]));
            e.setFechaFuente(Fechas.hoy());
            boolean exacto = f.length > 10 && Boolean.parseBoolean(f[10]);
            String extra = f.length > 11 ? f[11].trim() : "";
            e.setNotas("Carga inicial 10-10-2026. "
                    + (exacto ? "Fecha de inicio verificada en la fuente." : "Fecha de inicio aproximada.")
                    + (extra.isEmpty() ? "" : " " + extra)
                    + (Boolean.parseBoolean(f[7]) ? " REVISAR: tipo de lesión no precisado en la fuente." : "")
                    + (lado == Episodio.Lado.NO_APLICA && tipo.getZona().isBilateral() ? " Lado sin confirmar." : ""));
            episodios.save(e);
            nuevos++;
        }
        if (nuevos + existentes + omitidos + cerrados > 0) {
            log.info("lesiones.csv: {} nuevas, {} cerradas por alta, {} ya existentes, {} omitidas",
                    nuevos, cerrados, existentes, omitidos);
        }
    }

    /** Nombre corto de la web a partir de la URL (solo el dominio). */
    private static String nombreFuente(String url) {
        String host = url.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        int i = host.indexOf('/');
        return i > 0 ? host.substring(0, i) : host;
    }

    private List<String[]> leer(String ruta) throws IOException {
        try (var br = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8))) {
            return br.lines().skip(1).map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.split(";", -1)).toList();
        }
    }
}
