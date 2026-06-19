package com.aiq.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.codex.CodexCliProperties;
import com.aiq.infrastructure.executor.codex.CodexCommandBuilder;
import com.aiq.infrastructure.executor.codex.CodexOutputParser;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CodexCliPromptExecutorTest {

    private final ProcessRunner processRunner = mock(ProcessRunner.class);
    private final CodexCliProperties properties = properties();
    private final CodexCliPromptExecutor executor = new CodexCliPromptExecutor(
        processRunner,
        new CodexCommandBuilder(properties),
        new CodexOutputParser()
    );

    @Test
    void shouldBuildSafeCommand() {
        String command = executor.buildCommand(request());

        assertThat(command)
            .isEqualTo("codex exec --json --color never --sandbox workspace-write --skip-git-repo-check -")
            .doesNotContain("Implement executor");
    }

    @Test
    void shouldExecuteCodexCommandThroughProcessRunner() {
        when(processRunner.run(any())).thenReturn(new ProcessRunResult(
            0,
            "done",
            "",
            "done",
            Duration.ofMillis(100),
            false,
            null
        ));

        ExecutionResult result = executor.execute(request());

        ArgumentCaptor<ProcessCommand> commandCaptor = ArgumentCaptor.forClass(ProcessCommand.class);
        verify(processRunner).run(commandCaptor.capture());
        assertThat(commandCaptor.getValue().arguments()).containsExactly(
            "codex",
            "exec",
            "--json",
            "--color",
            "never",
            "--sandbox",
            "workspace-write",
            "--skip-git-repo-check",
            "-"
        );
        assertThat(commandCaptor.getValue().stdin()).contains("Implement executor");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.stdout()).isEqualTo("done");
    }

    @Test
    void shouldMapFailedProcessResult() {
        when(processRunner.run(any())).thenReturn(new ProcessRunResult(
            1,
            "",
            "Codex failed",
            "Codex failed",
            Duration.ofMillis(100),
            false,
            null
        ));

        ExecutionResult result = executor.execute(request());

        assertThat(result.isFailed()).isTrue();
        assertThat(result.errorMessage()).isEqualTo("Codex failed");
    }

    private CodexCliProperties properties() {
        CodexCliProperties properties = new CodexCliProperties();
        properties.setExecutablePath(Path.of("codex"));
        properties.setDefaultArguments(List.of(
            "exec",
            "--json",
            "--color",
            "never",
            "--sandbox",
            "workspace-write",
            "--skip-git-repo-check",
            "-"
        ));
        properties.setTimeout(Duration.ofSeconds(30));
        properties.setMaxOutputBytes(10_000);

        return properties;
    }

    private PromptExecutionRequest request() {
        return new PromptExecutionRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Codex executor",
            "Implement executor",
            "/tmp/project"
        );
    }
}
