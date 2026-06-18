package com.aiq.adapters.web.queue.request;

import com.aiq.domain.queue.AutoRunMode;
import com.aiq.domain.queue.QueueExecutionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

@Schema(description = "Queue execution policy")
public record QueueExecutionPolicyRequest(
    @Schema(description = "Automatic execution mode", example = "ASK_CONFIRMATION")
    @NotNull(message = "autoRunMode must not be null")
    AutoRunMode autoRunMode,

    @Schema(description = "Maximum prompts executed in one run", example = "3", minimum = "1")
    @Positive(message = "maxPromptsPerRun must be positive")
    int maxPromptsPerRun,

    @Schema(description = "Cooldown between prompt executions in ISO-8601 duration format", example = "PT1M")
    @NotNull(message = "cooldown must not be null")
    Duration cooldown,

    @Schema(description = "Stop queue execution after the first failed prompt", example = "true")
    boolean stopOnError,

    @Schema(description = "Restrict execution to configured working hours", example = "false")
    boolean workingHoursEnabled,

    @Schema(description = "Working hours settings. Required when workingHoursEnabled is true")
    @Valid
    WorkingHoursRequest workingHours
) {

    @AssertTrue(message = "cooldown must not be negative")
    public boolean isCooldownValid() {
        return cooldown == null || !cooldown.isNegative();
    }

    @AssertTrue(message = "workingHours must be set when working hours are enabled")
    public boolean isWorkingHoursValid() {
        return !workingHoursEnabled || workingHours != null;
    }

    public QueueExecutionPolicy toDomain() {
        return new QueueExecutionPolicy(
            autoRunMode,
            maxPromptsPerRun,
            cooldown,
            stopOnError,
            workingHoursEnabled,
            workingHours == null ? null : workingHours.toDomain()
        );
    }
}
