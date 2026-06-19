package com.aiq.adapters.web.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WebCorsProperties.class)
public class WebCorsConfig implements WebMvcConfigurer {

    private final WebCorsProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration registration = registry.addMapping("/api/**")
            .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
            .allowedOriginPatterns(properties.allowedOriginPatterns().toArray(String[]::new));

        registration.allowedMethods(properties.allowedMethods().toArray(String[]::new))
            .allowedHeaders(properties.allowedHeaders().toArray(String[]::new))
            .maxAge(properties.maxAgeSeconds());
    }
}
