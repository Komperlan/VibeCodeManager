package com.aiq.infrastructure.executor.codex;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CodexCommandBuilderTest {

    @Test
    void shouldBuildProcessCommandForCodexExec() {
        CodexCliProperties properties = properties();
        CodexCommandBuilder builder = new CodexCommandBuilder(properties);
        PromptExecutionRequest request = request("/tmp/project");

        ProcessCommand command = builder.build(request);

        assertThat(command.arguments()).containsExactly(
            "/usr/local/bin/codex",
            "exec",
            "--json",
            "--color",
            "never",
            "--sandbox",
            "workspace-write",
            "--skip-git-repo-check",
            "-"
        );
        assertThat(command.workingDirectory()).isEqualTo(Path.of("/tmp/project").toAbsolutePath().normalize());
        assertThat(command.stdin())
            .contains("Fix Maven tests")
            .contains("Run tests and fix failures");
        assertThat(command.environment()).containsEntry("NO_COLOR", "1");
        assertThat(command.timeout()).isEqualTo(Duration.ofMinutes(10));
        assertThat(command.maxOutputBytes()).isEqualTo(50_000);
    }

    @Test
    void shouldBuildSafeCommandWithoutPromptContent() {
        CodexCommandBuilder builder = new CodexCommandBuilder(properties());

        String command = builder.buildSafeCommand(request("/tmp/project"));

        assertThat(command)
            .isEqualTo("/usr/local/bin/codex exec --json --color never --sandbox workspace-write --skip-git-repo-check -")
            .doesNotContain("Run tests and fix failures");
    }

    @Test
    void shouldBuildResumeCommandWhenRequestHasCodexSession() {
        CodexCommandBuilder builder = new CodexCommandBuilder(properties());
        PromptExecutionRequest request = request(
            "/tmp/project",
            "019edddb-7d00-7df2-8577-d74b168adfad"
        );

        ProcessCommand command = builder.build(request);

        assertThat(command.arguments()).containsExactly(
            "/usr/local/bin/codex",
            "--sandbox",
            "workspace-write",
            "exec",
            "resume",
            "--json",
            "--skip-git-repo-check",
            "019edddb-7d00-7df2-8577-d74b168adfad",
            "-"
        );
        assertThat(builder.buildSafeCommand(request))
            .contains("exec resume")
            .contains("019edddb-7d00-7df2-8577-d74b168adfad")
            .doesNotContain("Run tests and fix failures");
    }

    @Test
    void shouldUseCurrentDirectoryWhenRequestHasNoWorkingDirectoryOverride() {
        CodexCommandBuilder builder = new CodexCommandBuilder(properties());

        ProcessCommand command = builder.build(request(null));

        assertThat(command.workingDirectory()).isEqualTo(Path.of("").toAbsolutePath().normalize());
    }

    @Test
    void shouldExpandHomeDirectoryInWorkingDirectoryOverride() {
        CodexCommandBuilder builder = new CodexCommandBuilder(properties());

        ProcessCommand command = builder.build(request("~/testDirForVCManager"));

        assertThat(command.workingDirectory())
            .isEqualTo(Path.of(System.getProperty("user.home"), "testDirForVCManager").toAbsolutePath().normalize());
    }

    @Test
    void shouldExpandHomeDirectoryInExecutablePath() {
        CodexCliProperties properties = properties();
        properties.setExecutablePath(Path.of("~/bin/codex"));
        CodexCommandBuilder builder = new CodexCommandBuilder(properties);

        ProcessCommand command = builder.build(request("/tmp/project"));

        assertThat(command.arguments().getFirst())
            .isEqualTo(Path.of(System.getProperty("user.home"), "bin/codex").toString());
    }

    private CodexCliProperties properties() {
        CodexCliProperties properties = new CodexCliProperties();
        properties.setExecutablePath(Path.of("/usr/local/bin/codex"));
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
        properties.setEnvironment(Map.of("NO_COLOR", "1"));
        properties.setTimeout(Duration.ofMinutes(10));
        properties.setMaxOutputBytes(50_000);

        return properties;
    }

    private PromptExecutionRequest request(String workingDirectoryOverride) {
        return request(workingDirectoryOverride, null);
    }

    private PromptExecutionRequest request(String workingDirectoryOverride, String codexSessionId) {
        return new PromptExecutionRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Fix Maven tests",
            "Run tests and fix failures",
            workingDirectoryOverride,
            codexSessionId
        );
    }
}
