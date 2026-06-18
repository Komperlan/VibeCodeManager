package com.aiq.adapters.web.project.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for changing a project root directory")
public record ChangeProjectRootDirectoryRequest(
    @Schema(description = "New absolute project root directory", example = "/home/user/projects/new-root")
    @NotBlank(message = "Project root directory must not be blank")
    String rootDirectory
) {
}
