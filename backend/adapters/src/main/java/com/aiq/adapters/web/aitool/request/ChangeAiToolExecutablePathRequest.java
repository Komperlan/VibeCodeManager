package com.aiq.adapters.web.aitool.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for changing an AI tool executable path")
public record ChangeAiToolExecutablePathRequest(
    @Schema(description = "New executable path or command", example = "/usr/local/bin/claude")
    @NotBlank(message = "executable path must not be blank")
    String executablePath
) {
}
