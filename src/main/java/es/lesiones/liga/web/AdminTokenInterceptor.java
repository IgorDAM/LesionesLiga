package es.lesiones.liga.web;

import es.lesiones.liga.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protección mínima y suficiente para un proyecto personal con un único administrador:
 * la cabecera X-Admin-Token debe coincidir con la variable de entorno ADMIN_TOKEN.
 * Tras varios intentos fallidos desde la misma dirección se responde 429 (ver {@link LimiteIntentos}).
 * (Usa HTTPS al desplegar; Render y similares ya lo dan.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminTokenInterceptor implements HandlerInterceptor {

    static final String TOKEN_POR_DEFECTO = "cambia-esto";

    private final AppProperties props;
    private final LimiteIntentos limite;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String cliente = request.getRemoteAddr();

        if (limite.bloqueado(cliente)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(limite.segundosRestantes(cliente)));
            responderJson(response, "Demasiados intentos fallidos. Espera unos minutos.");
            return false;
        }

        String esperado = props.adminToken();
        String recibido = request.getHeader("X-Admin-Token");

        boolean valido = esperado != null && !esperado.isBlank() && recibido != null
                && MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8)); // comparación en tiempo constante

        if (!valido) {
            limite.registrarFallo(cliente);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responderJson(response, "Token de administración incorrecto");
            return false;
        }
        limite.registrarAcierto(cliente);
        return true;
    }

    private static void responderJson(HttpServletResponse response, String mensaje) throws java.io.IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
