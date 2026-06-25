package com.aiq.adapters.web.prompt.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request for changing prompt position")
public record ChangePromptPositionRequest(
    @Schema(description = "New prompt position inside queue", example = "3", minimum = "0")
    @PositiveOrZero(message = "position must be positive or 0")
    long position
) {
}
