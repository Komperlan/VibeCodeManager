package com.aiq.adapters.web.prompt.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for changing a prompt title")
public record ChangePromptTitleRequest(
    @Schema(description = "New prompt title", example = "Refactor queue runner", maxLength = 150)
    @NotBlank(message = "title must not be blank")
    @Size(max = 150, message = "title must be at most 150 characters")
    String title
) {
}
