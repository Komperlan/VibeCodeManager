package com.aiq.domain.queue;

import com.aiq.domain.safety.WorkingHours;
import java.time.Duration;
import java.util.Objects;

public record QueueExecutionPolicy(
    AutoRunMode autoRunMode,
    int maxPromptsPerRun,
    Duration cooldown,
    boolean stopOnError,
    boolean workingHoursEnabled,
    WorkingHours workingHours
) {

    private static final int DEFAULT_MAX_PROMPTS_PER_RUN = 3;
    private static final Duration DEFAULT_COOLDOWN = Duration.ofSeconds(60);

    public QueueExecutionPolicy {
        Objects.requireNonNull(autoRunMode, "Auto-run mode must not be null");
        Objects.requireNonNull(cooldown, "Cooldown must not be null");
        if (maxPromptsPerRun <= 0) {
            throw new IllegalArgumentException("Max prompts per run must be positive");
        }
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("Cooldown must not be negative");
        }
        if (workingHoursEnabled && workingHours == null) {
            throw new IllegalArgumentException("Working hours must be set when working hours are enabled");
        }
    }

    public static QueueExecutionPolicy defaultPolicy() {
        return new QueueExecutionPolicy(
            AutoRunMode.ASK_CONFIRMATION,
            DEFAULT_MAX_PROMPTS_PER_RUN,
            DEFAULT_COOLDOWN,
            true,
            false,
            null
        );
    }
}