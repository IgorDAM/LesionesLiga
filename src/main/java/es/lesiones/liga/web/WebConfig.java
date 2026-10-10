package es.lesiones.liga.web;

import es.lesiones.liga.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminTokenInterceptor tokenInterceptor;
    private final AppProperties props;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/api/admin/**");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void avisarDeTokenPorDefecto() {
        if (AdminTokenInterceptor.TOKEN_POR_DEFECTO.equals(props.adminToken())) {
            log.warn("ADMIN_TOKEN no está definido: se usa el token por defecto. Defínelo antes de publicar la web.");
        }
    }
}
