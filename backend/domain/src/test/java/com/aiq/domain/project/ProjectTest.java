package com.aiq.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void shouldCreateActiveProjectWithTrimmedNameAndRootDirectory() {
        Project project = Project.create(
            "  Vibe Code Manager  ",
            "  /work/vibe-code-manager  "
        );

        assertThat(project.getId()).isNotNull();
        assertThat(project.getName()).isEqualTo("Vibe Code Manager");
        assertThat(project.getRootDirectory()).isEqualTo("/work/vibe-code-manager");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.isActive()).isTrue();
    }

    @Test
    void shouldRejectBlankProjectRootDirectory() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Project.create("Vibe Code Manager", " \t "))
            .withMessage("Project root directory must not be blank");
    }

    @Test
    void shouldNotRequireProjectRootDirectoryToExist() {
        String nonExistingPath = "/path/that/does/not/need/to/exist";

        Project project = Project.create("Vibe Code Manager", nonExistingPath);

        assertThat(project.getRootDirectory()).isEqualTo(nonExistingPath);
    }

    @Test
    void shouldRenameAndChangeRootDirectory() {
        Project project = project();

        project.rename("Renamed");
        project.changeRootDirectory("/new/root");

        assertThat(project.getName()).isEqualTo("Renamed");
        assertThat(project.getRootDirectory()).isEqualTo("/new/root");
    }

    @Test
    void shouldAttachAndClearCodexSession() {
        Project project = project();

        project.attachCodexSession("  019edddb-7d00-7df2-8577-d74b168adfad  ");

        assertThat(project.getCodexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");
        assertThat(project.hasCodexSession()).isTrue();

        project.clearCodexSession();

        assertThat(project.getCodexSessionId()).isNull();
        assertThat(project.hasCodexSession()).isFalse();
    }

    @Test
    void shouldCreateProjectWithExistingCodexSession() {
        Project project = Project.create(
            "Vibe Code Manager",
            "/work/vibe-code-manager",
            "  019edddb-7d00-7df2-8577-d74b168adfad  "
        );

        assertThat(project.getCodexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");
    }

    @Test
    void shouldRejectBlankCodexSession() {
        Project project = project();

        assertThatIllegalArgumentException()
            .isThrownBy(() -> project.attachCodexSession(" \t "))
            .withMessage("Project Codex session id must not be blank");
    }

    @Test
    void shouldDisableActivateAndArchiveProject() {
        Project project = project();

        project.disable();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DISABLED);

        project.activate();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);

        project.archive();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void shouldRejectChangingArchivedProject() {
        Project project = project();
        project.archive();

        assertThatIllegalStateException()
            .isThrownBy(() -> project.rename("New name"))
            .withMessage("Archived project cannot be changed");
    }

    @Test
    void shouldRejectRestoreWithUpdatedAtBeforeCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-02T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> Project.restore(
                UUID.randomUUID(),
                "Project",
                "/project",
                ProjectStatus.ACTIVE,
                createdAt,
                updatedAt
            ))
            .withMessage("Project updatedAt must not be before createdAt");
    }

    private Project project() {
        return Project.create("Vibe Code Manager", "/work/vibe-code-manager");
    }
}
