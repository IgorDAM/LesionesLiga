package es.lesiones.liga.web;

import es.lesiones.liga.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    static final int LONGITUD_RECOMENDADA = 16;

    private final AdminTokenInterceptor tokenInterceptor;
    private final AppProperties props;
    private final Environment entorno;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/api/admin/**");
    }

    /**
     * En producción la app se niega a arrancar sin un ADMIN_TOKEN propio: con el valor por defecto
     * cualquiera podría escribir en la base de datos.
     */
    @PostConstruct
    void exigirTokenEnProduccion() {
        String token = props.adminToken();
        boolean sinToken = token == null || token.isBlank() || AdminTokenInterceptor.TOKEN_POR_DEFECTO.equals(token);
        if (entorno.acceptsProfiles(Profiles.of("prod")) && sinToken) {
            throw new IllegalStateException(
                    "ADMIN_TOKEN no está definido (o es el valor por defecto) y el perfil es 'prod'. "
                            + "Define la variable de entorno ADMIN_TOKEN con una clave larga y aleatoria.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void avisarDeTokenDebil() {
        String token = props.adminToken();
        if (AdminTokenInterceptor.TOKEN_POR_DEFECTO.equals(token)) {
            log.warn("ADMIN_TOKEN no está definido: se usa el token por defecto. Solo válido en local.");
        } else if (token != null && token.length() < LONGITUD_RECOMENDADA) {
            log.warn("ADMIN_TOKEN tiene menos de {} caracteres: conviene una clave más larga.", LONGITUD_RECOMENDADA);
        }
    }
}
