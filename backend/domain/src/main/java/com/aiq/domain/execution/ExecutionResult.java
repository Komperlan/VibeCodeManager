package com.aiq.domain.execution;

public record ExecutionResult(
    int exitCode,
    String stdout,
    String stderr,
    String rawOutput,
    String errorMessage,
    String externalSessionId
) {

    public ExecutionResult(
        int exitCode,
        String stdout,
        String stderr,
        String rawOutput,
        String errorMessage
    ) {
        this(exitCode, stdout, stderr, rawOutput, errorMessage, null);
    }

    public ExecutionResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        rawOutput = rawOutput == null ? "" : rawOutput;
        externalSessionId = normalizeExternalSessionId(externalSessionId);
    }

    public boolean isSuccessful() {
        return exitCode == 0;
    }

    public boolean isFailed() {
        return !isSuccessful();
    }

    private static String normalizeExternalSessionId(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
