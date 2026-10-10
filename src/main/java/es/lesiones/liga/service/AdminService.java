package es.lesiones.liga.service;

import es.lesiones.liga.domain.*;
import es.lesiones.liga.domain.Episodio.Lado;
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

/** Escrituras del panel de administración (la única persona que escribe eres tú, una vez por semana). */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private final ClubRepository clubes;
    private final JugadorRepository jugadores;
    private final ZonaRepository zonas;
    private final TipoRepository tipos;
    private final EpisodioRepository episodios;

    // ---------- Clubes ----------

    public ClubDto crearClub(NuevoClub r) {
        if (clubes.findByNombreIgnoreCase(r.nombre().trim()).isPresent()) {
            throw Errores.conflicto("Ya existe un club con ese nombre");
        }
        Club c = new Club();
        aplicarClub(c, r);
        return Mapper.club(clubes.save(c));
    }

    public ClubDto editarClub(Long id, NuevoClub r) {
        Club c = clubes.findById(id).orElseThrow(() -> Errores.noEncontrado("Club no encontrado"));
        clubes.findByNombreIgnoreCase(r.nombre().trim()).filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw Errores.conflicto("Ya existe otro club con ese nombre");
                });
        aplicarClub(c, r);
        return Mapper.club(c);
    }

    public ClubDto marcarRevisado(Long id) {
        Club c = clubes.findById(id).orElseThrow(() -> Errores.noEncontrado("Club no encontrado"));
        c.setUltimaRevision(Fechas.hoy());
        return Mapper.club(c);
    }

    private void aplicarClub(Club c, NuevoClub r) {
        c.setNombre(r.nombre().trim());
        c.setDivision(r.division());
        c.setUrlOficial(Textos.vacioANull(r.urlOficial()));
        c.setUrlParteMedico(Textos.vacioANull(r.urlParteMedico()));
    }

    // ---------- Jugadores ----------

    public JugadorDto crearJugador(NuevoJugador r) {
        Club club = clubes.findById(r.clubId()).orElseThrow(() -> Errores.noEncontrado("Club no encontrado"));
        if (jugadores.existsByNombreIgnoreCaseAndClub_Id(r.nombre().trim(), club.getId())) {
            throw Errores.conflicto("Ese jugador ya existe en ese club");
        }
        Jugador j = new Jugador();
        j.setNombre(r.nombre().trim());
        j.setClub(club);
        j.setPosicion(r.posicion());
        j.setFechaNacimiento(r.fechaNacimiento());
        return Mapper.jugador(jugadores.save(j));
    }

    /** Un fichaje: el jugador conserva su historial y solo cambia de club. */
    public JugadorDto cambiarClub(Long jugadorId, Long clubId) {
        Jugador j = jugadores.findById(jugadorId).orElseThrow(() -> Errores.noEncontrado("Jugador no encontrado"));
        Club club = clubes.findById(clubId).orElseThrow(() -> Errores.noEncontrado("Club no encontrado"));
        j.setClub(club);
        return Mapper.jugador(j);
    }

    // ---------- Catálogo ----------

    public TipoDto crearTipo(NuevoTipo r) {
        ZonaCorporal zona = zonas.findById(r.zona()).orElseThrow(() -> Errores.noEncontrado("Zona no encontrada"));
        if (tipos.existsByZonaCodigoAndNombreIgnoreCase(zona.getCodigo(), r.nombre().trim())) {
            throw Errores.conflicto("Ese tipo de lesión ya existe en esa zona");
        }
        TipoLesion t = new TipoLesion();
        t.setZona(zona);
        t.setNombre(r.nombre().trim());
        t.setCategoria(r.categoria());
        t.setGravedad(r.gravedad());
        return Mapper.tipo(tipos.save(t));
    }

    // ---------- Episodios ----------

    public EpisodioDto crearEpisodio(NuevoEpisodio r) {
        Episodio e = new Episodio();
        aplicarEpisodio(e, r);
        if (e.getFechaAlta() == null && episodios.existsByJugadorIdAndTipoIdAndLadoAndFechaAltaIsNull(
                e.getJugador().getId(), e.getTipo().getId(), e.getLado())) {
            throw Errores.conflicto("Ese jugador ya tiene abierta esa misma lesión; dala de alta o edítala");
        }
        return Mapper.episodio(episodios.save(e), Fechas.hoy());
    }

    public EpisodioDto actualizarEpisodio(Long id, NuevoEpisodio r) {
        Episodio e = episodios.findById(id).orElseThrow(() -> Errores.noEncontrado("Episodio no encontrado"));
        aplicarEpisodio(e, r);
        return Mapper.episodio(e, Fechas.hoy());
    }

    public EpisodioDto darAlta(Long id, Alta r) {
        Episodio e = episodios.findById(id).orElseThrow(() -> Errores.noEncontrado("Episodio no encontrado"));
        if (r.fechaAlta().isBefore(e.getFechaInicio())) {
            throw Errores.malaPeticion("La fecha de alta no puede ser anterior al inicio de la lesión");
        }
        if (r.fechaAlta().isAfter(Fechas.hoy())) {
            throw Errores.malaPeticion("La fecha de alta no puede ser futura");
        }
        e.setFechaAlta(r.fechaAlta());
        if (r.partidosPerdidos() != null) {
            e.setPartidosPerdidos(r.partidosPerdidos());
        }
        return Mapper.episodio(e, Fechas.hoy());
    }

    public void borrarEpisodio(Long id) {
        if (!episodios.existsById(id)) {
            throw Errores.noEncontrado("Episodio no encontrado");
        }
        episodios.deleteById(id);
    }

    private void aplicarEpisodio(Episodio e, NuevoEpisodio r) {
        Jugador jugador = jugadores.findById(r.jugadorId())
                .orElseThrow(() -> Errores.noEncontrado("Jugador no encontrado"));
        TipoLesion tipo = tipos.findById(r.tipoId())
                .orElseThrow(() -> Errores.noEncontrado("Tipo de lesión no encontrado"));

        // El lado solo tiene sentido en zonas bilaterales; en el resto se fuerza NO_APLICA.
        Lado lado = r.lado();
        if (tipo.getZona().isBilateral()) {
            if (lado == null || lado == Lado.NO_APLICA) {
                throw Errores.malaPeticion("Indica el lado (derecho o izquierdo) para la zona " + tipo.getZona().getNombre());
            }
        } else {
            lado = Lado.NO_APLICA;
        }

        LocalDate hoy = Fechas.hoy();
        if (r.fechaInicio().isAfter(hoy)) {
            throw Errores.malaPeticion("La fecha de inicio no puede ser futura");
        }
        if (r.fechaAlta() != null) {
            if (r.fechaAlta().isBefore(r.fechaInicio())) {
                throw Errores.malaPeticion("La fecha de alta no puede ser anterior al inicio de la lesión");
            }
            if (r.fechaAlta().isAfter(hoy)) {
                throw Errores.malaPeticion("La fecha de alta no puede ser futura");
            }
        }

        e.setJugador(jugador);
        e.setTipo(tipo);
        e.setLado(lado);
        e.setFechaInicio(r.fechaInicio());
        e.setFechaAlta(r.fechaAlta());
        e.setPartidosPerdidos(r.partidosPerdidos());
        e.setRecaida(r.recaida());
        e.setCirugia(r.cirugia());
        e.setDiagnosticoClub(Textos.vacioANull(r.diagnosticoClub()));
        e.setPlazoEstimadoDias(r.plazoEstimadoDias());
        e.setFuenteUrl(r.fuenteUrl().trim());
        e.setFuenteNombre(Textos.vacioANull(r.fuenteNombre()));
        e.setFechaFuente(r.fechaFuente());
        e.setNotas(Textos.vacioANull(r.notas()));
    }

    // ---------- Exportación ----------

    /** CSV con ';' y BOM para que Excel en español lo abra bien. Sirve de copia de seguridad. */
    @Transactional(readOnly = true)
    public String exportarCsv() {
        LocalDate hoy = Fechas.hoy();
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("id;jugador;club;division;zona;tipo;lado;fecha_inicio;fecha_alta;dias_baja;partidos_perdidos;")
                .append("recaida;cirugia;plazo_estimado_dias;diagnostico_club;fuente_url;fuente_nombre;fecha_fuente;notas\n");
        for (Episodio e : episodios.findAll()) {
            var d = Mapper.episodio(e, hoy);
            long dias = ChronoUnit.DAYS.between(e.getFechaInicio(), e.getFechaAlta() != null ? e.getFechaAlta() : hoy);
            sb.append(String.join(";",
                    celda(d.id()), celda(d.jugador()), celda(d.club()), celda(d.division()), celda(d.zona()),
                    celda(d.tipo()), celda(d.lado()), celda(d.fechaInicio()), celda(d.fechaAlta()), celda(dias),
                    celda(d.partidosPerdidos()), celda(d.recaida()), celda(d.cirugia()), celda(d.plazoEstimadoDias()),
                    celda(d.diagnosticoClub()), celda(d.fuenteUrl()), celda(d.fuenteNombre()), celda(d.fechaFuente()),
                    celda(d.notas()))).append('\n');
        }
        return sb.toString();
    }

    /** Escapa comillas y neutraliza fórmulas (=, +, -, @) para que Excel no las ejecute. */
    private static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        String s = valor.toString().replace("\r", " ").replace("\n", " ");
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+")) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
