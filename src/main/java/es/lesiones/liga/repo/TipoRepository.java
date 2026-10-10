package es.lesiones.liga.repo;

import es.lesiones.liga.domain.TipoLesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoRepository extends JpaRepository<TipoLesion, Long> {

    List<TipoLesion> findAllByOrderByZonaOrdenAscNombreAsc();

    List<TipoLesion> findByZonaCodigoOrderByNombreAsc(String zona);

    boolean existsByZonaCodigoAndNombreIgnoreCase(String zona, String nombre);
}
