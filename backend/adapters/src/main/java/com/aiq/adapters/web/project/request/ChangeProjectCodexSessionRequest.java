package com.aiq.adapters.web.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for attaching or clearing a Codex session for a project")
public record ChangeProjectCodexSessionRequest(
    @Schema(
        description = "Existing Codex session/thread id. Use null to clear the project context.",
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
