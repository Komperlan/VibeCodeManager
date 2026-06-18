package com.aiq.adapters.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Vibe Code Manager API")
                .version("0.0.1")
                .description("Local-first API for managing projects, AI tools, prompt queues, prompts and queue runs."))
            .servers(List.of(new Server()
                .url("/")
                .description("Current server")));
    }
}
