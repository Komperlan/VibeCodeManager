package com.aiq.adapters.web.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiq.web.cors")
public record WebCorsProperties(
    List<String> allowedOrigins,
    List<String> allowedOriginPatterns,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    long maxAgeSeconds
) {

    public WebCorsProperties {
        allowedOrigins = defaultIfEmpty(allowedOrigins, List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://[::1]:5173"
        ));
        allowedOriginPatterns = defaultIfEmpty(allowedOriginPatterns, List.of(
            "http://localhost:[*]",
            "http://127.0.0.1:[*]"
        ));
        allowedMethods = defaultIfEmpty(allowedMethods, List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));
        allowedHeaders = defaultIfEmpty(allowedHeaders, List.of(
            "Content-Type",
            "Accept",
            "Authorization"
        ));
        if (maxAgeSeconds <= 0) {
            maxAgeSeconds = 3600;
        }
    }

    private static List<String> defaultIfEmpty(List<String> value, List<String> defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : List.copyOf(value);
    }
}
