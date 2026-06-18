package com.aiq.adapters.web.queue.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional request body for stopping a queue")
public record StopQueueRequest(
    @Schema(description = "Reason why the queue is stopped", example = "Manual stop before maintenance", maxLength = 500)
    @Size(max = 500, message = "reason must be at most 500 characters")
    String reason
) {

    @AssertTrue(message = "reason must not be blank")
    public boolean isReasonValid() {
        return reason == null || !reason.isBlank();
    }
}
