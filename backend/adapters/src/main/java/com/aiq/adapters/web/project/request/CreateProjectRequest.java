package com.aiq.adapters.web.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for creating a project")
public record CreateProjectRequest(
    @Schema(description = "Project display name", example = "Vibe Code Manager")
    @NotBlank(message = "Project name must not be blank")
    @Size(max = 100, message = "Project name must be at most 100 characters")
    String name,

    @Schema(description = "Absolute root directory of the project", example = "/home/user/projects/vibe-code-manager")
    @NotBlank(message = "Project root directory must not be blank")
    String rootDirectory,

    @Schema(
        description = "Optional existing Codex session/thread id. Leave empty to create a new context on first run.",
        example = "019edddb-7d00-7df2-8577-d74b168adfad"
    )
    @Size(max = 200, message = "Project Codex session id must be at most 200 characters")
    String codexSessionId
) {

    @AssertTrue(message = "Project Codex session id must not be blank")
    public boolean isCodexSessionIdNotBlankWhenPresent() {
        return codexSessionId == null || !codexSessionId.isBlank();
    }
}
