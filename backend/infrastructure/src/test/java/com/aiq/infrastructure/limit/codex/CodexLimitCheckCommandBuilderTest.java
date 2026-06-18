package com.aiq.infrastructure.limit.codex;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.infrastructure.executor.codex.CodexCliProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CodexLimitCheckCommandBuilderTest {

    @Test
    void shouldBuildProbeCommandWithoutPromptContent() {
        CodexLimitCheckerProperties limitProperties = new CodexLimitCheckerProperties();
        limitProperties.setProbePrompt("probe only");
        limitProperties.setTimeout(Duration.ofSeconds(10));
        limitProperties.setMaxOutputBytes(1000);
        limitProperties.setArguments(List.of("exec", "--json", "-"));

        CodexCliProperties codexProperties = new CodexCliProperties();
        codexProperties.setExecutablePath(Path.of("codex"));
        codexProperties.getEnvironment().put("SAFE_ENV", "value");

        CodexLimitCheckCommandBuilder builder = new CodexLimitCheckCommandBuilder(codexProperties, limitProperties);

        var command = builder.build(new AiLimitCheckRequest(
            UUID.randomUUID(),
            AiToolType.CODEX,
            "ignored-by-codex-properties",
            "/tmp/project"
        ));

        assertThat(command.arguments()).containsExactly("codex", "exec", "--json", "-");
        assertThat(command.stdin()).isEqualTo("probe only");
        assertThat(command.environment()).containsEntry("SAFE_ENV", "value");
        assertThat(command.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(command.maxOutputBytes()).isEqualTo(1000);
        assertThat(builder.buildSafeCommand(new AiLimitCheckRequest(
            UUID.randomUUID(),
            AiToolType.CODEX,
            "codex",
            "/tmp/project"
        ))).doesNotContain("probe only");
    }
}
