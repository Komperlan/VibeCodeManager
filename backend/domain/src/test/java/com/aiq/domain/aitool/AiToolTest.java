package com.aiq.domain.aitool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiToolTest {

    @Test
    void shouldCreateEnabledToolWithTrimmedNameAndExecutablePath() {
        AiTool tool = AiTool.create(
            "  Local Codex  ",
            AiToolType.CODEX,
            "  /usr/local/bin/codex  "
        );

        assertThat(tool.getId()).isNotNull();
        assertThat(tool.getName()).isEqualTo("Local Codex");
        assertThat(tool.getType()).isEqualTo(AiToolType.CODEX);
        assertThat(tool.getExecutablePath()).isEqualTo("/usr/local/bin/codex");
        assertThat(tool.getStatus()).isEqualTo(AiToolStatus.ENABLED);
        assertThat(tool.isEnabled()).isTrue();
        assertThat(tool.getCreatedAt()).isNotNull();
        assertThat(tool.getUpdatedAt()).isNotNull();
        assertThat(tool.getUpdatedAt()).isAfterOrEqualTo(tool.getCreatedAt());
    }

    @Test
    void shouldRestoreTool() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");

        AiTool tool = AiTool.restore(
            id,
            "Claude Code",
            AiToolStatus.DISABLED,
            AiToolType.CLAUDE_CODE,
            "/usr/local/bin/claude",
            createdAt,
            updatedAt
        );

        assertThat(tool.getId()).isEqualTo(id);
        assertThat(tool.getName()).isEqualTo("Claude Code");
        assertThat(tool.getStatus()).isEqualTo(AiToolStatus.DISABLED);
        assertThat(tool.getType()).isEqualTo(AiToolType.CLAUDE_CODE);
        assertThat(tool.getExecutablePath()).isEqualTo("/usr/local/bin/claude");
        assertThat(tool.getCreatedAt()).isEqualTo(createdAt);
        assertThat(tool.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(tool.isEnabled()).isFalse();
    }

    @Test
    void shouldRenameTool() {
        AiTool tool = codexTool();

        tool.rename("  Primary Codex  ");

        assertThat(tool.getName()).isEqualTo("Primary Codex");
        assertThat(tool.getUpdatedAt()).isAfterOrEqualTo(tool.getCreatedAt());
    }

    @Test
    void shouldChangeExecutablePath() {
        AiTool tool = codexTool();

        tool.changeExecutablePath("  /opt/codex/bin/codex  ");

        assertThat(tool.getExecutablePath()).isEqualTo("/opt/codex/bin/codex");
        assertThat(tool.getUpdatedAt()).isAfterOrEqualTo(tool.getCreatedAt());
    }

    @Test
    void shouldDisableAndEnableTool() {
        AiTool tool = codexTool();

        tool.disable();
        assertThat(tool.getStatus()).isEqualTo(AiToolStatus.DISABLED);
        assertThat(tool.isEnabled()).isFalse();

        tool.enable();
        assertThat(tool.getStatus()).isEqualTo(AiToolStatus.ENABLED);
        assertThat(tool.isEnabled()).isTrue();
    }

    @Test
    void shouldRejectNullName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> AiTool.create(null, AiToolType.CODEX, "/usr/local/bin/codex"))
            .withMessage("AiTool name must not be null");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> AiTool.create(" \t ", AiToolType.CODEX, "/usr/local/bin/codex"))
            .withMessage("AiTool name must not be blank");
    }

    @Test
    void shouldRejectTooLongName() {
        String name = "a".repeat(101);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> AiTool.create(name, AiToolType.CODEX, "/usr/local/bin/codex"))
            .withMessage("AiTool name must be at most 100 characters");
    }

    @Test
    void shouldRejectNullStatusOnRestore() {
        assertThatNullPointerException()
            .isThrownBy(() -> AiTool.restore(
                UUID.randomUUID(),
                "Codex",
                null,
                AiToolType.CODEX,
                "/usr/local/bin/codex",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
            ))
            .withMessage("AiTool status must not be null");
    }

    @Test
    void shouldRejectNullType() {
        assertThatNullPointerException()
            .isThrownBy(() -> AiTool.create("Codex", null, "/usr/local/bin/codex"))
            .withMessage("AiTool type must not be null");
    }

    @Test
    void shouldRejectNullExecutablePath() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> AiTool.create("Codex", AiToolType.CODEX, null))
            .withMessage("AiTool executable path must not be null");
    }

    @Test
    void shouldRejectBlankExecutablePath() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> AiTool.create("Codex", AiToolType.CODEX, " \t "))
            .withMessage("AiTool executable path must not be blank");
    }

    @Test
    void shouldRejectRestoreWithUpdatedAtBeforeCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-02T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> AiTool.restore(
                UUID.randomUUID(),
                "Codex",
                AiToolStatus.ENABLED,
                AiToolType.CODEX,
                "/usr/local/bin/codex",
                createdAt,
                updatedAt
            ))
            .withMessage("AiTool updatedAt must not be before createdAt");
    }

    @Test
    void shouldNotChangeToolWhenRenameIsInvalid() {
        AiTool tool = codexTool();
        String name = tool.getName();
        Instant updatedAt = tool.getUpdatedAt();

        assertThatIllegalArgumentException()
            .isThrownBy(() -> tool.rename(" "))
            .withMessage("AiTool name must not be blank");

        assertThat(tool.getName()).isEqualTo(name);
        assertThat(tool.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNotChangeToolWhenExecutablePathIsInvalid() {
        AiTool tool = codexTool();
        String executablePath = tool.getExecutablePath();
        Instant updatedAt = tool.getUpdatedAt();

        assertThatIllegalArgumentException()
            .isThrownBy(() -> tool.changeExecutablePath(" "))
            .withMessage("AiTool executable path must not be blank");

        assertThat(tool.getExecutablePath()).isEqualTo(executablePath);
        assertThat(tool.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private AiTool codexTool() {
        return AiTool.create("Codex", AiToolType.CODEX, "/usr/local/bin/codex");
    }
}
