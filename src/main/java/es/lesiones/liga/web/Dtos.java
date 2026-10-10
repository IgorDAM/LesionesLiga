package es.lesiones.liga.web;

import es.lesiones.liga.domain.Club.Division;
import es.lesiones.liga.domain.Episodio.Lado;
import es.lesiones.liga.domain.Jugador.Posicion;
import es.lesiones.liga.domain.TipoLesion.Categoria;
import es.lesiones.liga.domain.TipoLesion.Gravedad;
import es.lesiones.liga.domain.ZonaCorporal.Vista;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Todos los objetos que entran y salen por la API, agrupados para no dispersar ficheros. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Salida ----------

    public record ClubDto(Long id, String nombre, Division division, String urlOficial,
                          String urlParteMedico, LocalDate ultimaRevision) {
    }

    public record ZonaDto(String codigo, String nombre, Vista vista, boolean bilateral) {
    }

    public record TipoDto(Long id, String zona, String zonaNombre, String nombre,
                          Categoria categoria, Gravedad gravedad) {
    }

    public record JugadorDto(Long id, String nombre, Long clubId, String club, Division division,
                             Posicion posicion, LocalDate fechaNacimiento) {
    }

    public record EpisodioDto(Long id, Long jugadorId, String jugador, String club, Division division,
                              Long tipoId, String tipo, String zonaCodigo, String zona, Lado lado,
                              LocalDate fechaInicio, LocalDate fechaAlta, long diasBaja, boolean activa,
                              Integer partidosPerdidos, boolean recaida, boolean cirugia,
                              String diagnosticoClub, Integer plazoEstimadoDias, LocalDate regresoEstimado,
                              String fuenteUrl, String fuenteNombre, LocalDate fechaFuente, String notas) {
    }

    public record FichaJugadorDto(JugadorDto jugador, List<EpisodioDto> activas, List<EpisodioDto> historial) {
    }

    public record ResumenZonaDto(String zona, Lado lado, long activos) {
    }

    public record ResumenDto(long lesionadosActivos, long primera, long segunda, long casosCerrados,
                             Instant ultimaActualizacion, List<ResumenZonaDto> porZona) {
    }

    public record ResumenTipoDto(Long tipoId, String tipo, Gravedad gravedad, int casosCerrados,
                                 int casosActivos, Double medianaDias, Double mediaDias) {
    }

    public record EstadisticaDto(Long tipoId, String tipo, String zonaCodigo, String zona,
                                 int casosCerrados, int casosActivos,
                                 Double mediaDias, Double medianaDias, Long minDias, Long maxDias,
                                 Double mediaPartidos,
                                 int casosConEstimacion, Double desvioMedioVsEstimado,
                                 boolean pocosCasos, int minimoCasos,
                                 List<EpisodioDto> casos) {
    }

    // ---------- Entrada (panel de administración) ----------

    public record NuevoClub(@NotBlank @Size(max = 120) String nombre,
                            @NotNull Division division,
                            @Size(max = 300) String urlOficial,
                            @Size(max = 300) String urlParteMedico) {
    }

    public record NuevoJugador(@NotBlank @Size(max = 120) String nombre,
                               @NotNull Long clubId,
                               Posicion posicion,
                               LocalDate fechaNacimiento) {
    }

    public record NuevoTipo(@NotBlank String zona,
                            @NotBlank @Size(max = 120) String nombre,
                            @NotNull Categoria categoria,
                            @NotNull Gravedad gravedad) {
    }

    public record NuevoEpisodio(@NotNull Long jugadorId,
                                @NotNull Long tipoId,
                                Lado lado,
                                @NotNull LocalDate fechaInicio,
                                LocalDate fechaAlta,
                                @Min(0) Integer partidosPerdidos,
                                boolean recaida,
                                boolean cirugia,
                                @Size(max = 500) String diagnosticoClub,
                                @Min(1) Integer plazoEstimadoDias,
                                @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "debe ser una URL http(s)") String fuenteUrl,
                                @Size(max = 120) String fuenteNombre,
                                LocalDate fechaFuente,
                                @Size(max = 1000) String notas) {
    }

    public record Alta(@NotNull LocalDate fechaAlta, @Min(0) Integer partidosPerdidos) {
    }
}
