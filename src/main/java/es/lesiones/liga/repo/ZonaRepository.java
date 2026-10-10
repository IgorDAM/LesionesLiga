package es.lesiones.liga.repo;

import es.lesiones.liga.domain.ZonaCorporal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaRepository extends JpaRepository<ZonaCorporal, String> {

    List<ZonaCorporal> findAllByOrderByOrdenAsc();
}
