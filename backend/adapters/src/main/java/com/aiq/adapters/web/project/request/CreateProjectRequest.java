package com.aiq.adapters.web.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    String rootDirectory
) {
}
