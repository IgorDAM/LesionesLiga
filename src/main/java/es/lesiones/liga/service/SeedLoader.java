package es.lesiones.liga.service;

import es.lesiones.liga.domain.Club;
import es.lesiones.liga.domain.TipoLesion;
import es.lesiones.liga.domain.ZonaCorporal;
import es.lesiones.liga.repo.ClubRepository;
import es.lesiones.liga.repo.TipoRepository;
import es.lesiones.liga.repo.ZonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carga los CSV de src/main/resources/seed al arrancar. Es idempotente: solo inserta lo que falta,
 * así que puedes añadir una línea a tipos.csv y reiniciar sin duplicar nada.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SeedLoader implements CommandLineRunner {

    private final ZonaRepository zonas;
    private final TipoRepository tipos;
    private final ClubRepository clubes;

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        int nuevasZonas = 0, nuevosTipos = 0, nuevosClubes = 0;

        for (String[] f : leer("seed/zonas.csv")) {
            if (!zonas.existsById(f[0])) {
                ZonaCorporal z = new ZonaCorporal();
                z.setCodigo(f[0]);
                z.setNombre(f[1]);
                z.setVista(ZonaCorporal.Vista.valueOf(f[2]));
                z.setBilateral(Boolean.parseBoolean(f[3]));
                z.setOrden(Integer.parseInt(f[4]));
                zonas.save(z);
                nuevasZonas++;
            }
        }

        Map<String, ZonaCorporal> porCodigo = zonas.findAll().stream()
                .collect(Collectors.toMap(ZonaCorporal::getCodigo, Function.identity()));
        for (String[] f : leer("seed/tipos.csv")) {
            ZonaCorporal zona = porCodigo.get(f[0]);
            if (zona == null) {
                log.warn("tipos.csv: zona desconocida '{}' (línea ignorada)", f[0]);
                continue;
            }
            if (!tipos.existsByZonaCodigoAndNombreIgnoreCase(f[0], f[1])) {
                TipoLesion t = new TipoLesion();
                t.setZona(zona);
                t.setNombre(f[1]);
                t.setCategoria(TipoLesion.Categoria.valueOf(f[2]));
                t.setGravedad(TipoLesion.Gravedad.valueOf(f[3]));
                tipos.save(t);
                nuevosTipos++;
            }
        }

        for (String[] f : leer("seed/clubes.csv")) {
            if (clubes.findByNombreIgnoreCase(f[0]).isEmpty()) {
                Club c = new Club();
                c.setNombre(f[0]);
                c.setDivision(Club.Division.valueOf(f[1]));
                clubes.save(c);
                nuevosClubes++;
            }
        }

        if (nuevasZonas + nuevosTipos + nuevosClubes > 0) {
            log.info("Datos semilla cargados: {} zonas, {} tipos de lesión, {} clubes", nuevasZonas, nuevosTipos, nuevosClubes);
        }
    }

    private List<String[]> leer(String ruta) throws IOException {
        try (var br = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8))) {
            return br.lines()
                    .skip(1) // cabecera
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.split(";", -1))
                    .toList();
        }
    }
}
