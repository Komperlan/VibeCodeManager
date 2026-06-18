package com.aiq.adapters.web.prompt.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for changing prompt content")
public record ChangePromptContentRequest(
    @Schema(description = "New prompt content", example = "Update the REST controller tests.", maxLength = 50_000)
    @NotBlank(message = "content must not be blank")
    @Size(max = 50_000, message = "content must be at most 50000 characters")
    String content
) {
}
