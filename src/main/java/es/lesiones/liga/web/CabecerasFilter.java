package es.lesiones.liga.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cabeceras de seguridad para todas las respuestas y política de caché de la API:
 * los datos públicos cambian una vez por semana, así que se cachean unos minutos (menos CPU en el plan F1
 * y menos despertares de la base serverless); la API de administración nunca se cachea.
 */
@Component
public class CabecerasFilter extends OncePerRequestFilter {

    /** Los estilos en línea (style="...") del frontend obligan a permitir 'unsafe-inline' solo en estilos. */
    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    static final String CACHE_PUBLICA = "public, max-age=120";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("Content-Security-Policy", CSP);
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        res.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");

        String ruta = req.getRequestURI();
        if (ruta.startsWith("/api/admin")) {
            res.setHeader("Cache-Control", "no-store");
        } else if (ruta.startsWith("/api/") && "GET".equals(req.getMethod())) {
            res.setHeader("Cache-Control", CACHE_PUBLICA);
        }
        chain.doFilter(req, res);
    }
}
