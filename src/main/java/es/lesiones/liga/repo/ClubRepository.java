package es.lesiones.liga.repo;

import es.lesiones.liga.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {

    Optional<Club> findByNombreIgnoreCase(String nombre);

    List<Club> findAllByOrderByDivisionAscNombreAsc();
}
