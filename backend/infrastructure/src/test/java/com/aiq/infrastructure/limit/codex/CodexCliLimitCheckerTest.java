package com.aiq.infrastructure.limit.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitStatus;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.ProcessRunner;
import com.aiq.infrastructure.executor.codex.CodexCliProperties;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CodexCliLimitCheckerTest {

    private final ProcessRunner processRunner = mock(ProcessRunner.class);
    private final CodexCliProperties codexProperties = codexProperties();
    private final CodexLimitCheckerProperties limitProperties = limitProperties();
    private final CodexCliLimitChecker checker = new CodexCliLimitChecker(
        processRunner,
        new CodexLimitCheckCommandBuilder(codexProperties, limitProperties),
        new CodexLimitOutputParser(),
        limitProperties
    );

    @Test
    void shouldRunProbeCommandThroughProcessRunner() {
        when(processRunner.run(any())).thenReturn(new ProcessRunResult(
            0,
            "AVAILABLE",
            "",
            "AVAILABLE",
            Duration.ofMillis(100),
            false,
            null
        ));

        var result = checker.checkLimit(request(AiToolType.CODEX));

        ArgumentCaptor<ProcessCommand> commandCaptor = ArgumentCaptor.forClass(ProcessCommand.class);
        verify(processRunner).run(commandCaptor.capture());
        ProcessCommand command = commandCaptor.getValue();
        assertThat(command.arguments()).containsExactly(
            "codex",
            "exec",
            "--json",
            "--color",
            "never",
            "--skip-git-repo-check",
            "--ephemeral",
            "-"
        );
        assertThat(command.stdin()).isEqualTo(limitProperties.getProbePrompt());
        assertThat(command.stdin()).doesNotContain("real prompt");
        assertThat(command.workingDirectory()).isEqualTo(Path.of("/tmp/project").toAbsolutePath().normalize());
        assertThat(result.status()).isEqualTo(AiLimitStatus.AVAILABLE);
    }

    @Test
    void shouldRejectNonCodexToolWithoutRunningProcess() {
        var result = checker.checkLimit(request(AiToolType.FAKE));

        assertThat(result.status()).isEqualTo(AiLimitStatus.ERROR);
        assertThat(result.reason()).contains("supports only CODEX tools");
        verify(processRunner, never()).run(any());
    }

    @Test
    void shouldFailOpenOnTechnicalProbeErrorByDefault() {
        when(processRunner.run(any())).thenReturn(new ProcessRunResult(
            2,
            "",
            "auth failed",
            "auth failed",
            Duration.ofMillis(100),
            false,
            null
        ));

        var result = checker.checkLimit(request(AiToolType.CODEX));

        assertThat(result.status()).isEqualTo(AiLimitStatus.AVAILABLE);
    }

    @Test
    void shouldReturnTechnicalProbeErrorWhenFailOpenIsDisabled() {
        CodexLimitCheckerProperties strictLimitProperties = limitProperties();
        strictLimitProperties.setFailOpenOnError(false);
        CodexCliLimitChecker strictChecker = new CodexCliLimitChecker(
            processRunner,
            new CodexLimitCheckCommandBuilder(codexProperties, strictLimitProperties),
            new CodexLimitOutputParser(),
            strictLimitProperties
        );
        when(processRunner.run(any())).thenReturn(new ProcessRunResult(
            2,
            "",
            "auth failed",
            "auth failed",
            Duration.ofMillis(100),
            false,
            null
        ));

        var result = strictChecker.checkLimit(request(AiToolType.CODEX));

        assertThat(result.status()).isEqualTo(AiLimitStatus.ERROR);
        assertThat(result.reason()).isEqualTo("auth failed");
    }

    @Test
    void shouldDetectLimitInExecutorResultWithoutStartingProbe() {
        var result = checker.detectLimit(
            request(AiToolType.CODEX),
            new ExecutionResult(1, "", "usage limit reached", "usage limit reached", null)
        );

        assertThat(result).hasValueSatisfying(limit ->
            assertThat(limit.status()).isEqualTo(AiLimitStatus.LIMIT_REACHED)
        );
        verify(processRunner, never()).run(any());
    }

    private CodexCliProperties codexProperties() {
        CodexCliProperties properties = new CodexCliProperties();
        properties.setExecutablePath(Path.of("codex"));
        return properties;
    }

    private CodexLimitCheckerProperties limitProperties() {
        CodexLimitCheckerProperties properties = new CodexLimitCheckerProperties();
        properties.setTimeout(Duration.ofSeconds(30));
        properties.setMaxOutputBytes(100_000);
        properties.setArguments(List.of(
            "exec",
            "--json",
            "--color",
            "never",
            "--skip-git-repo-check",
            "--ephemeral",
            "-"
        ));
        properties.setProbePrompt("Reply exactly AVAILABLE. Do not inspect files, run commands, or modify anything.");
        return properties;
    }

    private AiLimitCheckRequest request(AiToolType type) {
        return new AiLimitCheckRequest(
            UUID.randomUUID(),
            type,
            "codex",
            "/tmp/project"
        );
    }
}
