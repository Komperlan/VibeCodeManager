package com.aiq.adapters.web.aitool.controller;

import com.aiq.adapters.web.aitool.request.ChangeAiToolExecutablePathRequest;
import com.aiq.adapters.web.aitool.request.CreateAiToolRequest;
import com.aiq.adapters.web.aitool.request.RenameAiToolRequest;
import com.aiq.adapters.web.common.ErrorResponse;
import com.aiq.application.aitool.CreateAiToolCommand;
import com.aiq.application.aitool.dto.AiToolDetails;
import com.aiq.application.aitool.dto.AiToolSummary;
import com.aiq.application.aitool.dto.CreateAiToolResult;
import com.aiq.application.service.AiToolApplicationService;
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
@RequestMapping("/api/v1/ai-tools")
@RequiredArgsConstructor
@Tag(name = "AI Tools", description = "AI tool registry and lifecycle operations")
public class AiToolController {

    private final AiToolApplicationService aiToolApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create AI tool", description = "Registers an AI tool executable for prompt execution.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "AI tool created"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CreateAiToolResult createAiTool(@Valid @RequestBody CreateAiToolRequest request) {
        return aiToolApplicationService.createAiTool(
            new CreateAiToolCommand(request.name(), request.type(), request.executablePath())
        );
    }

    @GetMapping("/{aiToolId}")
    @Operation(summary = "Get AI tool details", description = "Returns AI tool configuration by id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI tool found"),
        @ApiResponse(responseCode = "400", description = "Invalid AI tool id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AiToolDetails getAiTool(
        @Parameter(description = "AI tool id", example = "d47f57db-9f12-48cb-a7f2-6b0e37bf3a18")
        @PathVariable UUID aiToolId
    ) {
        return aiToolApplicationService.getAiTool(aiToolId);
    }

    @GetMapping
    @Operation(summary = "List AI tools", description = "Returns all registered AI tools.")
    @ApiResponse(responseCode = "200", description = "AI tools returned")
    public List<AiToolSummary> listAiTools() {
        return aiToolApplicationService.listAiTools();
    }

    @PatchMapping("/{aiToolId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Rename AI tool", description = "Changes an AI tool display name.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "AI tool renamed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void renameAiTool(
        @Parameter(description = "AI tool id", example = "d47f57db-9f12-48cb-a7f2-6b0e37bf3a18")
        @PathVariable UUID aiToolId,
        @Valid @RequestBody RenameAiToolRequest request
    ) {
        aiToolApplicationService.renameAiTool(aiToolId, request.name());
    }

    @PatchMapping("/{aiToolId}/executable-path")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change AI tool executable path", description = "Changes the executable path or command used to run the AI tool.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Executable path changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changeExecutablePath(
        @Parameter(description = "AI tool id", example = "d47f57db-9f12-48cb-a7f2-6b0e37bf3a18")
        @PathVariable UUID aiToolId,
        @Valid @RequestBody ChangeAiToolExecutablePathRequest request
    ) {
        aiToolApplicationService.changeExecutablePath(aiToolId, request.executablePath());
    }

    @PostMapping("/{aiToolId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Enable AI tool", description = "Allows the AI tool to be used by prompts.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "AI tool enabled", content = @Content),
        @ApiResponse(responseCode = "404", description = "AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void enableAiTool(
        @Parameter(description = "AI tool id", example = "d47f57db-9f12-48cb-a7f2-6b0e37bf3a18")
        @PathVariable UUID aiToolId
    ) {
        aiToolApplicationService.enableAiTool(aiToolId);
    }

    @PostMapping("/{aiToolId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disable AI tool", description = "Prevents the AI tool from being used by new prompt runs.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "AI tool disabled", content = @Content),
        @ApiResponse(responseCode = "404", description = "AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void disableAiTool(
        @Parameter(description = "AI tool id", example = "d47f57db-9f12-48cb-a7f2-6b0e37bf3a18")
        @PathVariable UUID aiToolId
    ) {
        aiToolApplicationService.disableAiTool(aiToolId);
    }
}
