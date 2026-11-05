package es.lesiones.liga.web;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra la fuerza bruta sobre el token de administración: tras {@link #MAX_FALLOS} intentos
 * fallidos desde una misma dirección, esa dirección queda bloqueada {@link #VENTANA}. Vive en memoria
 * (suficiente para una sola instancia y un único administrador; se reinicia al reiniciar la app).
 */
@Component
public class LimiteIntentos {

    static final int MAX_FALLOS = 10;
    static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int MAX_ENTRADAS = 2_000;

    private record Registro(int fallos, long desdeMs) {
    }

    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();

    /** true si la dirección ha agotado sus intentos y todavía está dentro de la ventana de bloqueo. */
    public boolean bloqueado(String clave) {
        Registro r = registros.get(clave);
        if (r == null) {
            return false;
        }
        if (caducado(r)) {
            registros.remove(clave, r);
            return false;
        }
        return r.fallos() >= MAX_FALLOS;
    }

    public void registrarFallo(String clave) {
        if (registros.size() > MAX_ENTRADAS) {
            registros.values().removeIf(this::caducado); // evita crecer sin límite si alguien escanea
        }
        long ahora = System.currentTimeMillis();
        registros.merge(clave, new Registro(1, ahora), (viejo, nuevo) ->
                caducado(viejo) ? nuevo : new Registro(viejo.fallos() + 1, viejo.desdeMs()));
    }

    public void registrarAcierto(String clave) {
        registros.remove(clave);
    }

    public long segundosRestantes(String clave) {
        Registro r = registros.get(clave);
        if (r == null) {
            return 0;
        }
        long restante = VENTANA.toMillis() - (System.currentTimeMillis() - r.desdeMs());
        return Math.max(1, restante / 1000);
    }

    private boolean caducado(Registro r) {
        return System.currentTimeMillis() - r.desdeMs() > VENTANA.toMillis();
    }
}
