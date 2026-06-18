package com.aiq.adapters.web.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "API error response")
public record ErrorResponse(
    @Schema(description = "Stable application error code", example = "VALIDATION_ERROR")
    String code,

    @Schema(description = "Human-readable error message", example = "Request validation failed")
    String message,

    @Schema(description = "Field-level or request-level error details")
    Map<String, List<String>> details,

    @Schema(description = "Error timestamp in UTC")
    Instant timestamp
) {

    public static ErrorResponse of(String code, String message) {
        return of(code, message, Map.of());
    }

    public static ErrorResponse of(
        String code,
        String message,
        Map<String, List<String>> details
    ) {
        return new ErrorResponse(code, message, details == null ? Map.of() : details, Instant.now());
    }
}
