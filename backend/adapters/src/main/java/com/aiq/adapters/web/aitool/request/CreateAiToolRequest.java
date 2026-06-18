package com.aiq.adapters.web.aitool.request;

import com.aiq.domain.aitool.AiToolType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for registering an AI tool executable")
public record CreateAiToolRequest(
    @Schema(description = "Human-readable AI tool name", example = "Codex CLI")
    @NotBlank(message = "Tool name must not be blank")
    @Size(max = 100, message = "Tool name must be at most 100 characters")
    String name,

    @Schema(description = "AI tool integration type", example = "CODEX")
    @NotNull(message = "Tool type must not be null")
    AiToolType type,

    @Schema(description = "Executable path or command used to run the tool", example = "/usr/local/bin/codex")
    @NotBlank(message = "executable path must not be blank")
    String executablePath
) {
}
