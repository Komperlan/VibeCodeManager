package com.aiq.adapters.web.prompt.controller;

import com.aiq.adapters.web.prompt.request.AddPromptRequest;
import com.aiq.adapters.web.prompt.request.ChangePromptContentRequest;
import com.aiq.adapters.web.prompt.request.ChangePromptPriorityRequest;
import com.aiq.adapters.web.prompt.request.ChangePromptPositionRequest;
import com.aiq.adapters.web.prompt.request.ChangePromptTitleRequest;
import com.aiq.adapters.web.common.ErrorResponse;
import com.aiq.application.prompt.AddPromptCommand;
import com.aiq.application.prompt.dto.AddPromptResult;
import com.aiq.application.prompt.dto.PromptDetails;
import com.aiq.application.prompt.dto.PromptSummary;
import com.aiq.application.service.PromptApplicationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompts", description = "Prompt creation, editing and lifecycle operations")
public class PromptController {

    private final PromptApplicationService promptApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add queued prompt", description = "Creates a prompt in QUEUED status.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Prompt created"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue or AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AddPromptResult addPrompt(@Valid @RequestBody AddPromptRequest request) {
        return promptApplicationService.addPrompt(toCommand(request));
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add draft prompt", description = "Creates a prompt in DRAFT status.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Draft prompt created"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue or AI tool not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AddPromptResult addDraftPrompt(@Valid @RequestBody AddPromptRequest request) {
        return promptApplicationService.addDraftPrompt(toCommand(request));
    }

    @GetMapping("/{promptId}")
    @Operation(summary = "Get prompt details", description = "Returns prompt details by id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prompt found"),
        @ApiResponse(responseCode = "400", description = "Invalid prompt id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PromptDetails getPrompt(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId
    ) {
        return promptApplicationService.getPrompt(promptId);
    }

    @GetMapping
    @Operation(summary = "List queue prompts", description = "Returns prompts that belong to a queue ordered by execution order.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prompts returned"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid queue id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<PromptSummary> listQueuePrompts(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @RequestParam UUID queueId
    ) {
        return promptApplicationService.listQueuePrompts(queueId);
    }

    @PostMapping("/{promptId}/enqueue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Enqueue draft prompt", description = "Moves a DRAFT prompt to QUEUED status.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt enqueued", content = @Content),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow enqueue",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void enqueuePrompt(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId
    ) {
        promptApplicationService.enqueuePrompt(promptId);
    }

    @PatchMapping("/{promptId}/title")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change prompt title", description = "Changes prompt title while the prompt is editable.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt title changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow editing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changePromptTitle(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId,
        @Valid @RequestBody ChangePromptTitleRequest request
    ) {
        promptApplicationService.changePromptTitle(promptId, request.title());
    }

    @PatchMapping("/{promptId}/content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change prompt content", description = "Changes prompt content while the prompt is editable.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt content changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow editing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changePromptContent(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId,
        @Valid @RequestBody ChangePromptContentRequest request
    ) {
        promptApplicationService.changePromptContent(promptId, request.content());
    }

    @PatchMapping("/{promptId}/priority")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change prompt priority", description = "Changes prompt priority while the prompt is editable.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt priority changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow editing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changePromptPriority(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId,
        @Valid @RequestBody ChangePromptPriorityRequest request
    ) {
        promptApplicationService.changePromptPriority(promptId, request.priority());
    }

    @PatchMapping("/{promptId}/position")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change prompt position", description = "Moves a draft or queued prompt inside its queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt position changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow reordering",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changePromptPosition(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId,
        @Valid @RequestBody ChangePromptPositionRequest request
    ) {
        promptApplicationService.changePromptPosition(promptId, request.position());
    }

    @PostMapping("/{promptId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel prompt", description = "Cancels a non-terminal prompt.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt cancelled", content = @Content),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow cancelling",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void cancelPrompt(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId
    ) {
        promptApplicationService.cancelPrompt(promptId);
    }

    @PostMapping("/{promptId}/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Skip prompt", description = "Marks a non-terminal prompt as skipped.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Prompt skipped", content = @Content),
        @ApiResponse(responseCode = "404", description = "Prompt not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Prompt state does not allow skipping",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void skipPrompt(
        @Parameter(description = "Prompt id", example = "8f3fdc67-2b5d-40d9-a939-a7d4fc015e6e")
        @PathVariable UUID promptId
    ) {
        promptApplicationService.skipPrompt(promptId);
    }

    private AddPromptCommand toCommand(AddPromptRequest request) {
        return new AddPromptCommand(
            request.queueId(),
            request.targetAiToolId(),
            request.title(),
            request.content(),
            request.priority(),
            request.maxAttempts(),
            request.workingDirectoryOverride()
        );
    }
}
