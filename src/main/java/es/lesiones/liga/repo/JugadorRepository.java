package es.lesiones.liga.repo;

import es.lesiones.liga.domain.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    List<Jugador> findTop20ByNombreBusquedaContainingOrderByNombreAsc(String fragmento);

    boolean existsByNombreIgnoreCaseAndClub_Id(String nombre, Long clubId);
}
