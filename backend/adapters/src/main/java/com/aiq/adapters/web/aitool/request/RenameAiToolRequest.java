package com.aiq.adapters.web.aitool.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for renaming an AI tool")
public record RenameAiToolRequest(
    @Schema(description = "New AI tool name", example = "Claude Code")
    @NotBlank(message = "Tool name must not be blank")
    @Size(max = 100, message = "Tool name must be at most 100 characters")
    String name
) {
}
