package com.aiq.infrastructure.executor.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ProcessCommand(
    @NotEmpty(message = "process arguments must not be empty")
    List<@NotBlank(message = "process argument must not be blank") String> arguments,

    @NotNull(message = "working directory must not be null")
    Path workingDirectory,

    String stdin,

    @NotNull(message = "environment must not be null")
    Map<
        @NotBlank(message = "environment variable name must not be blank") String,
        @NotNull(message = "environment variable value must not be null") String
    > environment,

    @NotNull(message = "timeout must not be null")
    Duration timeout,

    @Positive(message = "max output bytes must be positive")
    int maxOutputBytes
) {

    @AssertTrue(message = "timeout must be positive")
    public boolean isTimeoutPositive() {
        return timeout == null || (!timeout.isZero() && !timeout.isNegative());
    }
}
