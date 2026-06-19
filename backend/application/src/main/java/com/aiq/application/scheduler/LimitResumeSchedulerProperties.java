package com.aiq.application.scheduler;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "aiq.queue.limit-resume")
public class LimitResumeSchedulerProperties {

    private boolean enabled = true;

    @NotNull(message = "limit resume poll interval must not be null")
    private Duration pollInterval = Duration.ofSeconds(60);

    @AssertTrue(message = "limit resume poll interval must be positive")
    public boolean isPollIntervalPositive() {
        return pollInterval == null || (!pollInterval.isZero() && !pollInterval.isNegative());
    }
}
