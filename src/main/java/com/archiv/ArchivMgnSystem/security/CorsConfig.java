package com.archiv.ArchivMgnSystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Legt fest, von welchen Webseiten aus der Browser die API aufrufen darf.
 *
 * Produktiv: APP_CORS_ALLOWED_ORIGINS auf die echte Adresse des Frontends
 * setzen (z.B. https://archiv.example.ch, mehrere mit Komma getrennt).
 *
 * Ohne diese Angabe sind nur Adressen erlaubt, die es ausschliesslich beim
 * lokalen Entwickeln gibt: localhost und private Netzwerkadressen (damit das
 * Testen mit dem Handy im WLAN funktioniert). Öffentliche Webseiten sind
 * damit in jedem Fall ausgeschlossen.
 */
@Configuration
public class CorsConfig {

    private static final Pattern LOCAL_DEVELOPMENT_ORIGIN = Pattern.compile(
            "^https?://(localhost|127\\.0\\.0\\.1"
                    + "|10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
                    + "|192\\.168\\.\\d{1,3}\\.\\d{1,3}"
                    + "|172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3})(:\\d{1,5})?$");

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProperty;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> configuredOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration;
        if (configuredOrigins.isEmpty()) {
            configuration = new CorsConfiguration() {
                @Override
                public String checkOrigin(String origin) {
                    return origin != null && LOCAL_DEVELOPMENT_ORIGIN.matcher(origin).matches() ? origin : null;
                }
            };
        } else {
            configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(configuredOrigins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
