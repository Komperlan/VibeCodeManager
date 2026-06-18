package com.aiq.adapters.web.project.controller;

import com.aiq.adapters.web.project.request.ChangeProjectRootDirectoryRequest;
import com.aiq.adapters.web.project.request.CreateProjectRequest;
import com.aiq.adapters.web.project.request.RenameProjectRequest;
import com.aiq.adapters.web.common.ErrorResponse;
import com.aiq.application.project.CreateProjectCommand;
import com.aiq.application.project.dto.CreateProjectResult;
import com.aiq.application.project.dto.ProjectDetails;
import com.aiq.application.project.dto.ProjectSummary;
import com.aiq.application.service.ProjectApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project registration and lifecycle operations")
public class ProjectController {

    private final ProjectApplicationService projectApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create project", description = "Registers a local project root directory.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Project created"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CreateProjectResult createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectApplicationService.createProject(
            new CreateProjectCommand(request.name(), request.rootDirectory())
        );
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project details", description = "Returns project details by id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project found"),
        @ApiResponse(responseCode = "400", description = "Invalid project id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProjectDetails getProject(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @PathVariable UUID projectId
    ) {
        return projectApplicationService.getProject(projectId);
    }

    @GetMapping
    @Operation(summary = "List projects", description = "Returns all registered projects.")
    @ApiResponse(responseCode = "200", description = "Projects returned")
    public List<ProjectSummary> listProjects() {
        return projectApplicationService.listProjects();
    }

    @PatchMapping("/{projectId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Rename project", description = "Changes the display name of a project.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project renamed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Project state does not allow renaming",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void renameProject(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @PathVariable UUID projectId,
        @Valid @RequestBody RenameProjectRequest request
    ) {
        projectApplicationService.renameProject(projectId, request.name());
    }

    @PatchMapping("/{projectId}/root-directory")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change project root directory", description = "Changes the local root directory of a project.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Root directory changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Project state does not allow changing root directory",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changeRootDirectory(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @PathVariable UUID projectId,
        @Valid @RequestBody ChangeProjectRootDirectoryRequest request
    ) {
        projectApplicationService.changeRootDirectory(projectId, request.rootDirectory());
    }

    @PostMapping("/{projectId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disable project", description = "Disables a project without deleting it.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project disabled", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Project state does not allow disabling",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void disableProject(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @PathVariable UUID projectId
    ) {
        projectApplicationService.disableProject(projectId);
    }

    @PostMapping("/{projectId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Activate project", description = "Activates a disabled project.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project activated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Project state does not allow activation",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void activateProject(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @PathVariable UUID projectId
    ) {
        projectApplicationService.activateProject(projectId);
    }

    @PostMapping("/{projectId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive project", description = "Archives a project and prevents further changes.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project archived", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void archiveProject(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @PathVariable UUID projectId
    ) {
        projectApplicationService.archiveProject(projectId);
    }
}
