package com.aiq.infrastructure.executor.request;

import java.time.Duration;

public record ProcessRunResult(
    int exitCode,
    String stdout,
    String stderr,
    String rawOutput,
    Duration duration,
    boolean timedOut,
    String errorMessage
) {

    public ProcessRunResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        rawOutput = rawOutput == null ? "" : rawOutput;
        duration = duration == null ? Duration.ZERO : duration;
    }
}
