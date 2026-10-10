package es.lesiones.liga.repo;

import es.lesiones.liga.domain.Episodio;
import es.lesiones.liga.domain.Episodio.Lado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface EpisodioRepository extends JpaRepository<Episodio, Long> {

    List<Episodio> findByJugadorIdOrderByFechaInicioDesc(Long jugadorId);

    List<Episodio> findByFechaAltaIsNullOrderByFechaInicioDesc();

    List<Episodio> findByTipoId(Long tipoId);

    List<Episodio> findByTipoZonaCodigo(String zona);

    boolean existsByJugadorIdAndTipoIdAndLadoAndFechaAltaIsNull(Long jugadorId, Long tipoId, Lado lado);

    long countByFechaAltaIsNotNull();

    @Query("select max(e.actualizadoEn) from Episodio e")
    Instant ultimaActualizacion();
}
