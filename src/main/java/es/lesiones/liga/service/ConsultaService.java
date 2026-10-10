package es.lesiones.liga.service;

import es.lesiones.liga.config.AppProperties;
import es.lesiones.liga.domain.Club.Division;
import es.lesiones.liga.domain.Episodio;
import es.lesiones.liga.domain.TipoLesion;
import es.lesiones.liga.repo.*;
import es.lesiones.liga.util.Errores;
import es.lesiones.liga.util.Fechas;
import es.lesiones.liga.util.Textos;
import es.lesiones.liga.web.Dtos.*;
import es.lesiones.liga.web.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Todo lo que lee el público: búsquedas, lesiones activas y estadísticas de recuperación. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsultaService {

    private final ZonaRepository zonas;
    private final TipoRepository tipos;
    private final ClubRepository clubes;
    private final JugadorRepository jugadores;
    private final EpisodioRepository episodios;
    private final AppProperties props;

    // ---------- Catálogos ----------

    public List<ZonaDto> zonas() {
        return zonas.findAllByOrderByOrdenAsc().stream().map(Mapper::zona).toList();
    }

    public List<TipoDto> tipos(String zona) {
        var lista = (zona == null || zona.isBlank())
                ? tipos.findAllByOrderByZonaOrdenAscNombreAsc()
                : tipos.findByZonaCodigoOrderByNombreAsc(zona);
        return lista.stream().map(Mapper::tipo).toList();
    }

    public List<ClubDto> clubes() {
        return clubes.findAllByOrderByDivisionAscNombreAsc().stream().map(Mapper::club).toList();
    }

    // ---------- Jugadores ----------

    public List<JugadorDto> buscarJugadores(String q) {
        String fragmento = Textos.normalizar(q);
        if (fragmento.length() < 2) {
            return List.of();
        }
        return jugadores.findTop20ByNombreBusquedaContainingOrderByNombreAsc(fragmento)
                .stream().map(Mapper::jugador).toList();
    }

    public FichaJugadorDto ficha(Long id) {
        var jugador = jugadores.findById(id)
                .orElseThrow(() -> Errores.noEncontrado("Jugador no encontrado"));
        LocalDate hoy = Fechas.hoy();
        var todos = episodios.findByJugadorIdOrderByFechaInicioDesc(id).stream()
                .map(e -> Mapper.episodio(e, hoy)).toList();
        return new FichaJugadorDto(Mapper.jugador(jugador),
                todos.stream().filter(EpisodioDto::activa).toList(),
                todos.stream().filter(e -> !e.activa()).toList());
    }

    // ---------- Lesiones activas ----------

    public List<EpisodioDto> activas(Division division, Long clubId, String zona) {
        LocalDate hoy = Fechas.hoy();
        return episodios.findByFechaAltaIsNullOrderByFechaInicioDesc().stream()
                .filter(e -> division == null || e.getJugador().getClub().getDivision() == division)
                .filter(e -> clubId == null || e.getJugador().getClub().getId().equals(clubId))
                .filter(e -> zona == null || zona.isBlank() || e.getTipo().getZona().getCodigo().equals(zona))
                .map(e -> Mapper.episodio(e, hoy))
                .toList();
    }

    public ResumenDto resumen() {
        var activas = episodios.findByFechaAltaIsNullOrderByFechaInicioDesc();
        long primera = activas.stream().filter(e -> e.getJugador().getClub().getDivision() == Division.PRIMERA).count();
        long segunda = activas.size() - primera;

        record Clave(String zona, Episodio.Lado lado) {
        }
        var porZona = activas.stream()
                .collect(Collectors.groupingBy(e -> new Clave(e.getTipo().getZona().getCodigo(), e.getLado()),
                        Collectors.counting()))
                .entrySet().stream()
                .map(en -> new ResumenZonaDto(en.getKey().zona(), en.getKey().lado(), en.getValue()))
                .toList();

        return new ResumenDto(activas.size(), primera, segunda, episodios.countByFechaAltaIsNotNull(),
                episodios.ultimaActualizacion(), porZona);
    }

    // ---------- Estadísticas de recuperación ----------

    /** Una fila por tipo de lesión de la zona, para elegir qué comparar. */
    public List<ResumenTipoDto> resumenZona(String codigoZona) {
        var porTipo = episodios.findByTipoZonaCodigo(codigoZona).stream()
                .collect(Collectors.groupingBy(e -> e.getTipo().getId()));
        List<ResumenTipoDto> filas = new ArrayList<>();
        for (TipoLesion t : tipos.findByZonaCodigoOrderByNombreAsc(codigoZona)) {
            var casos = porTipo.getOrDefault(t.getId(), List.of());
            var dias = diasCerrados(casos);
            filas.add(new ResumenTipoDto(t.getId(), t.getNombre(), t.getGravedad(), dias.size(),
                    casos.size() - dias.size(), mediana(dias), media(dias)));
        }
        return filas;
    }

    /**
     * Estadísticas de un tipo de lesión.
     *
     * @param cirugia          null = todos; true/false = solo con/sin cirugía
     * @param incluirRecaidas  si es false se descartan las recaídas (suelen tardar distinto)
     */
    public EstadisticaDto estadistica(Long tipoId, Boolean cirugia, boolean incluirRecaidas) {
        TipoLesion tipo = tipos.findById(tipoId)
                .orElseThrow(() -> Errores.noEncontrado("Tipo de lesión no encontrado"));
        LocalDate hoy = Fechas.hoy();

        var casos = episodios.findByTipoId(tipoId).stream()
                .filter(e -> cirugia == null || e.isCirugia() == cirugia)
                .filter(e -> incluirRecaidas || !e.isRecaida())
                .sorted(Comparator.comparing(Episodio::getFechaInicio).reversed())
                .toList();

        var cerrados = casos.stream().filter(e -> e.getFechaAlta() != null).toList();
        var dias = diasCerrados(casos);

        OptionalDouble partidos = cerrados.stream()
                .filter(e -> e.getPartidosPerdidos() != null)
                .mapToInt(Episodio::getPartidosPerdidos).average();
        Double mediaPartidos = partidos.isPresent() ? partidos.getAsDouble() : null;

        var conEstimacion = cerrados.stream().filter(e -> e.getPlazoEstimadoDias() != null).toList();
        Double desvio = conEstimacion.isEmpty() ? null : conEstimacion.stream()
                .mapToLong(e -> ChronoUnit.DAYS.between(e.getFechaInicio(), e.getFechaAlta()) - e.getPlazoEstimadoDias())
                .average().orElse(0);

        return new EstadisticaDto(tipo.getId(), tipo.getNombre(), tipo.getZona().getCodigo(), tipo.getZona().getNombre(),
                dias.size(), casos.size() - cerrados.size(),
                media(dias), mediana(dias),
                dias.isEmpty() ? null : dias.get(0), dias.isEmpty() ? null : dias.get(dias.size() - 1),
                mediaPartidos,
                conEstimacion.size(), desvio,
                dias.size() < props.minimoCasos(), props.minimoCasos(),
                casos.stream().map(e -> Mapper.episodio(e, hoy)).toList());
    }

    /** Días de baja de los episodios ya cerrados, ordenados de menor a mayor. */
    private static List<Long> diasCerrados(List<Episodio> casos) {
        return casos.stream()
                .filter(e -> e.getFechaAlta() != null)
                .map(e -> ChronoUnit.DAYS.between(e.getFechaInicio(), e.getFechaAlta()))
                .sorted()
                .toList();
    }

    private static Double media(List<Long> ordenados) {
        return ordenados.isEmpty() ? null
                : ordenados.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private static Double mediana(List<Long> ordenados) {
        int n = ordenados.size();
        if (n == 0) {
            return null;
        }
        return n % 2 == 1 ? ordenados.get(n / 2).doubleValue()
                : (ordenados.get(n / 2 - 1) + ordenados.get(n / 2)) / 2.0;
    }
}
