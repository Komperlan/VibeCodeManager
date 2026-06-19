package com.aiq.adapters.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WebCorsPropertiesTest {

    @Test
    void shouldUseLocalDevDefaults() {
        WebCorsProperties properties = new WebCorsProperties(null, null, null, null, 0);

        assertThat(properties.allowedOrigins())
            .containsExactly("http://localhost:5173", "http://127.0.0.1:5173", "http://[::1]:5173");
        assertThat(properties.allowedOriginPatterns())
            .containsExactly("http://localhost:[*]", "http://127.0.0.1:[*]");
        assertThat(properties.allowedMethods())
            .contains("GET", "POST", "PATCH", "OPTIONS");
        assertThat(properties.allowedHeaders())
            .contains("Content-Type", "Accept", "Authorization");
        assertThat(properties.maxAgeSeconds()).isEqualTo(3600);
    }

    @Test
    void shouldKeepConfiguredValues() {
        WebCorsProperties properties = new WebCorsProperties(
            List.of("http://localhost:3000"),
            List.of("http://127.0.0.1:[*]"),
            List.of("GET"),
            List.of("Content-Type"),
            60
        );

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:3000");
        assertThat(properties.allowedOriginPatterns()).containsExactly("http://127.0.0.1:[*]");
        assertThat(properties.allowedMethods()).containsExactly("GET");
        assertThat(properties.allowedHeaders()).containsExactly("Content-Type");
        assertThat(properties.maxAgeSeconds()).isEqualTo(60);
    }
}
