package es.lesiones.liga.web;

import es.lesiones.liga.service.AdminService;
import es.lesiones.liga.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Escrituras. Protegido por AdminTokenInterceptor (cabecera X-Admin-Token). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService admin;

    /** Sirve para que la web compruebe que el token es correcto. */
    @GetMapping("/ping")
    public Map<String, Boolean> ping() {
        return Map.of("ok", true);
    }

    // ---------- Clubes ----------

    @PostMapping("/clubes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubDto crearClub(@Valid @RequestBody NuevoClub r) {
        return admin.crearClub(r);
    }

    @PutMapping("/clubes/{id}")
    public ClubDto editarClub(@PathVariable Long id, @Valid @RequestBody NuevoClub r) {
        return admin.editarClub(id, r);
    }

    @PatchMapping("/clubes/{id}/revisado")
    public ClubDto marcarRevisado(@PathVariable Long id) {
        return admin.marcarRevisado(id);
    }

    // ---------- Jugadores ----------

    @PostMapping("/jugadores")
    @ResponseStatus(HttpStatus.CREATED)
    public JugadorDto crearJugador(@Valid @RequestBody NuevoJugador r) {
        return admin.crearJugador(r);
    }

    @PatchMapping("/jugadores/{id}/club")
    public JugadorDto cambiarClub(@PathVariable Long id, @RequestParam Long clubId) {
        return admin.cambiarClub(id, clubId);
    }

    // ---------- Catálogo ----------

    @PostMapping("/tipos")
    @ResponseStatus(HttpStatus.CREATED)
    public TipoDto crearTipo(@Valid @RequestBody NuevoTipo r) {
        return admin.crearTipo(r);
    }

    // ---------- Episodios ----------

    @PostMapping("/episodios")
    @ResponseStatus(HttpStatus.CREATED)
    public EpisodioDto crearEpisodio(@Valid @RequestBody NuevoEpisodio r) {
        return admin.crearEpisodio(r);
    }

    @PutMapping("/episodios/{id}")
    public EpisodioDto actualizarEpisodio(@PathVariable Long id, @Valid @RequestBody NuevoEpisodio r) {
        return admin.actualizarEpisodio(id, r);
    }

    @PatchMapping("/episodios/{id}/alta")
    public EpisodioDto darAlta(@PathVariable Long id, @Valid @RequestBody Alta r) {
        return admin.darAlta(id, r);
    }

    @DeleteMapping("/episodios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarEpisodio(@PathVariable Long id) {
        admin.borrarEpisodio(id);
    }

    // ---------- Copia de seguridad ----------

    @GetMapping(value = "/export/episodios.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportar() {
        byte[] cuerpo = admin.exportarCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"lesiones.csv\"")
                .body(cuerpo);
    }
}
