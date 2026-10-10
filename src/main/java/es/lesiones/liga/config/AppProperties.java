package es.lesiones.liga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades propias de la aplicación (prefijo "app" en application.properties).
 *
 * @param adminToken   token que debe enviarse en la cabecera X-Admin-Token para usar /api/admin/**
 * @param minimoCasos  por debajo de este número de casos cerrados, las estadísticas se marcan como poco fiables
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String adminToken, int minimoCasos) {
}
