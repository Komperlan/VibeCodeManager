package com.aiq.domain.execution;

public record ExecutionResult(
    int exitCode,
    String stdout,
    String stderr,
    String rawOutput,
    String errorMessage
) {

    public ExecutionResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        rawOutput = rawOutput == null ? "" : rawOutput;
    }

    public boolean isSuccessful() {
        return exitCode == 0;
    }

    public boolean isFailed() {
        return !isSuccessful();
    }
}
