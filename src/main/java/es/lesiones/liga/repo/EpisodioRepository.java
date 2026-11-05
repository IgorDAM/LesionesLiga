package es.lesiones.liga.repo;

import es.lesiones.liga.domain.Episodio;
import es.lesiones.liga.domain.Episodio.Lado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface EpisodioRepository extends JpaRepository<Episodio, Long> {

    /*
     * Las relaciones @ManyToOne son EAGER por defecto: sin @EntityGraph, cada episodio dispararía consultas
     * sueltas por su jugador, club, tipo y zona (N+1). Con él, todo viaja en una sola consulta con joins.
     */
    @Override
    @EntityGraph(attributePaths = {"jugador", "jugador.club", "tipo", "tipo.zona"})
    List<Episodio> findAll();

    @EntityGraph(attributePaths = {"jugador", "jugador.club", "tipo", "tipo.zona"})
    List<Episodio> findByJugadorIdOrderByFechaInicioDesc(Long jugadorId);

    @EntityGraph(attributePaths = {"jugador", "jugador.club", "tipo", "tipo.zona"})
    List<Episodio> findByFechaAltaIsNullOrderByFechaInicioDesc();

    @EntityGraph(attributePaths = {"jugador", "jugador.club", "tipo", "tipo.zona"})
    List<Episodio> findByTipoId(Long tipoId);

    @EntityGraph(attributePaths = {"jugador", "jugador.club", "tipo", "tipo.zona"})
    List<Episodio> findByTipoZonaCodigo(String zona);

    boolean existsByJugadorIdAndTipoIdAndLadoAndFechaAltaIsNull(Long jugadorId, Long tipoId, Lado lado);

    List<Episodio> findByJugadorIdAndTipoIdAndLadoAndFechaAltaIsNull(Long jugadorId, Long tipoId, Lado lado);

    List<Episodio> findByJugadorIdAndTipoIdAndLadoAndFechaInicio(Long jugadorId, Long tipoId, Lado lado,
                                                                 java.time.LocalDate fechaInicio);

    boolean existsByJugadorIdAndTipoIdAndLadoAndFechaInicio(Long jugadorId, Long tipoId, Lado lado,
                                                            java.time.LocalDate fechaInicio);

    long countByFechaAltaIsNotNull();

    @Query("select max(e.actualizadoEn) from Episodio e")
    Instant ultimaActualizacion();
}
