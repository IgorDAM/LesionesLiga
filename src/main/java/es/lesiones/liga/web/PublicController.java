package es.lesiones.liga.web;

import es.lesiones.liga.domain.Club.Division;
import es.lesiones.liga.service.ConsultaService;
import es.lesiones.liga.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API de solo lectura: es lo que consume la web pública. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final ConsultaService consulta;

    @GetMapping("/zonas")
    public List<ZonaDto> zonas() {
        return consulta.zonas();
    }

    @GetMapping("/tipos")
    public List<TipoDto> tipos(@RequestParam(required = false) String zona) {
        return consulta.tipos(zona);
    }

    @GetMapping("/clubes")
    public List<ClubDto> clubes() {
        return consulta.clubes();
    }

    @GetMapping("/jugadores")
    public List<JugadorDto> jugadores(@RequestParam(defaultValue = "") String q) {
        return consulta.buscarJugadores(q);
    }

    @GetMapping("/jugadores/{id}")
    public FichaJugadorDto ficha(@PathVariable Long id) {
        return consulta.ficha(id);
    }

    @GetMapping("/lesiones/activas")
    public List<EpisodioDto> activas(@RequestParam(required = false) Division division,
                                     @RequestParam(required = false) Long clubId,
                                     @RequestParam(required = false) String zona) {
        return consulta.activas(division, clubId, zona);
    }

    @GetMapping("/resumen")
    public ResumenDto resumen() {
        return consulta.resumen();
    }

    @GetMapping("/estadisticas/zona/{codigo}")
    public List<ResumenTipoDto> estadisticasZona(@PathVariable String codigo) {
        return consulta.resumenZona(codigo);
    }

    @GetMapping("/estadisticas/tipo/{id}")
    public EstadisticaDto estadisticasTipo(@PathVariable Long id,
                                           @RequestParam(required = false) Boolean cirugia,
                                           @RequestParam(defaultValue = "true") boolean incluirRecaidas) {
        return consulta.estadistica(id, cirugia, incluirRecaidas);
    }
}
