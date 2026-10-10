package es.lesiones.liga.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class Fechas {

    public static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private Fechas() {
    }

    /** "Hoy" siempre en hora de España, no en la del servidor (Render, por ejemplo, corre en UTC). */
    public static LocalDate hoy() {
        return LocalDate.now(MADRID);
    }
}
