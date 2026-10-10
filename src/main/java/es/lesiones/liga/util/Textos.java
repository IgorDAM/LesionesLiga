package es.lesiones.liga.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Textos {

    private Textos() {
    }

    /** Minúsculas y sin tildes, para búsquedas tolerantes ("Íñigo" == "inigo"). */
    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }

    public static String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
