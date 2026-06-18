package com.aiq.infrastructure.executor.codex;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CodexOutputParserTest {

    private final CodexOutputParser parser = new CodexOutputParser();

    @Test
    void shouldParseSuccessfulResult() {
        ProcessRunResult processResult = new ProcessRunResult(
            0,
            "done",
            "",
            "done",
            Duration.ofMillis(100),
            false,
            null
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.stdout()).isEqualTo("done");
        assertThat(result.stderr()).isEmpty();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void shouldExtractTextFromCodexJsonLineMessage() {
        String stdout = """
            {"type":"session.started","id":"session-id"}
            {"type":"message","message":"Implementation complete"}
            """;
        ProcessRunResult processResult = new ProcessRunResult(
            0,
            stdout,
            "",
            stdout,
            Duration.ofMillis(100),
            false,
            null
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.stdout()).isEqualTo("Implementation complete");
        assertThat(result.rawOutput()).isEqualTo(stdout);
    }

    @Test
    void shouldExtractTextFromNestedCodexJsonLineContent() {
        String stdout = """
            {"type":"agent_message","message":{"content":[{"type":"output_text","text":"Nested output"}]}}
            """;
        ProcessRunResult processResult = new ProcessRunResult(
            0,
            stdout,
            "",
            stdout,
            Duration.ofMillis(100),
            false,
            null
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.stdout()).isEqualTo("Nested output");
    }

    @Test
    void shouldFallbackToRawStdoutWhenJsonLinesDoNotContainText() {
        String stdout = """
            {"type":"session.started","id":"session-id"}
            {"type":"turn.completed","usage":{"input_tokens":10}}
            """;
        ProcessRunResult processResult = new ProcessRunResult(
            0,
            stdout,
            "",
            stdout,
            Duration.ofMillis(100),
            false,
            null
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.stdout()).isEqualTo(stdout);
    }

    @Test
    void shouldFallbackToRawStdoutWhenJsonLineIsMalformed() {
        String stdout = """
            {"type":"message","message":
            """;
        ProcessRunResult processResult = new ProcessRunResult(
            0,
            stdout,
            "",
            stdout,
            Duration.ofMillis(100),
            false,
            null
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.stdout()).isEqualTo(stdout);
    }

    @Test
    void shouldUseStderrAsErrorMessageForFailedExit() {
        ProcessRunResult processResult = new ProcessRunResult(
            2,
            "",
            "Codex failed",
            "Codex failed",
            Duration.ofMillis(100),
            false,
            null
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.errorMessage()).isEqualTo("Codex failed");
    }

    @Test
    void shouldUseRunnerErrorMessageWhenPresent() {
        ProcessRunResult processResult = new ProcessRunResult(
            -1,
            "",
            "",
            "",
            Duration.ofMillis(100),
            false,
            "Process output exceeded maxOutputBytes limit: 100"
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.errorMessage()).isEqualTo("Process output exceeded maxOutputBytes limit: 100");
    }

    @Test
    void shouldMapTimeoutToFailedExecutionResult() {
        ProcessRunResult processResult = new ProcessRunResult(
            -1,
            "",
            "",
            "",
            Duration.ofSeconds(30),
            true,
            "Process timed out after PT30S"
        );

        ExecutionResult result = parser.parse(processResult);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.errorMessage()).isEqualTo("Process timed out after PT30S");
    }
}
