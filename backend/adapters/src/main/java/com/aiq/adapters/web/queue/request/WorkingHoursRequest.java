package com.aiq.adapters.web.queue.request;

import com.aiq.domain.safety.WorkingHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.time.ZoneId;

@Schema(description = "Allowed queue execution time window")
public record WorkingHoursRequest(
    @Schema(description = "Start time in local time", example = "09:00:00")
    @NotNull(message = "workingHours.from must not be null")
    LocalTime from,

    @Schema(description = "End time in local time", example = "18:00:00")
    @NotNull(message = "workingHours.to must not be null")
    LocalTime to,

    @Schema(description = "IANA time zone id", example = "Europe/Moscow")
    @NotNull(message = "workingHours.zoneId must not be null")
    ZoneId zoneId
) {

    public WorkingHours toDomain() {
        return new WorkingHours(from, to, zoneId);
    }
}
