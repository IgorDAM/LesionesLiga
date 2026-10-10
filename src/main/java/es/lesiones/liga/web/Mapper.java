package es.lesiones.liga.web;

import es.lesiones.liga.domain.*;
import es.lesiones.liga.web.Dtos.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Entidad -> DTO. Es lo único que sale por la API; las entidades JPA nunca se serializan. */
public final class Mapper {

    private Mapper() {
    }

    public static ClubDto club(Club c) {
        return new ClubDto(c.getId(), c.getNombre(), c.getDivision(), c.getUrlOficial(),
                c.getUrlParteMedico(), c.getUltimaRevision());
    }

    public static ZonaDto zona(ZonaCorporal z) {
        return new ZonaDto(z.getCodigo(), z.getNombre(), z.getVista(), z.isBilateral());
    }

    public static TipoDto tipo(TipoLesion t) {
        return new TipoDto(t.getId(), t.getZona().getCodigo(), t.getZona().getNombre(), t.getNombre(),
                t.getCategoria(), t.getGravedad());
    }

    public static JugadorDto jugador(Jugador j) {
        Club c = j.getClub();
        return new JugadorDto(j.getId(), j.getNombre(), c.getId(), c.getNombre(), c.getDivision(),
                j.getPosicion(), j.getFechaNacimiento());
    }

    public static EpisodioDto episodio(Episodio e, LocalDate hoy) {
        Jugador j = e.getJugador();
        Club c = j.getClub();
        TipoLesion t = e.getTipo();
        ZonaCorporal z = t.getZona();
        boolean activa = e.getFechaAlta() == null;
        LocalDate fin = activa ? hoy : e.getFechaAlta();
        LocalDate regreso = (activa && e.getPlazoEstimadoDias() != null)
                ? e.getFechaInicio().plusDays(e.getPlazoEstimadoDias()) : null;
        return new EpisodioDto(e.getId(), j.getId(), j.getNombre(), c.getNombre(), c.getDivision(),
                t.getId(), t.getNombre(), z.getCodigo(), z.getNombre(), e.getLado(),
                e.getFechaInicio(), e.getFechaAlta(), ChronoUnit.DAYS.between(e.getFechaInicio(), fin), activa,
                e.getPartidosPerdidos(), e.isRecaida(), e.isCirugia(),
                e.getDiagnosticoClub(), e.getPlazoEstimadoDias(), regreso,
                e.getFuenteUrl(), e.getFuenteNombre(), e.getFechaFuente(), e.getNotas());
    }
}
