package com.aiq.adapters.web.queue.controller;

import com.aiq.adapters.web.queue.request.ChangeQueuePolicyRequest;
import com.aiq.adapters.web.queue.request.CreateQueueRequest;
import com.aiq.adapters.web.queue.request.StopQueueRequest;
import com.aiq.adapters.web.common.ErrorResponse;
import com.aiq.application.queue.ChangeQueuePolicyCommand;
import com.aiq.application.queue.CreateQueueCommand;
import com.aiq.application.queue.dto.CreateQueueResult;
import com.aiq.application.queue.dto.QueueDetails;
import com.aiq.application.queue.dto.QueueSummary;
import com.aiq.application.service.PromptQueueApplicationService;
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
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
@Tag(name = "Prompt Queues", description = "Prompt queue configuration and lifecycle operations")
public class PromptQueueController {

    private final PromptQueueApplicationService promptQueueApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create prompt queue", description = "Creates a prompt queue for a project.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Queue created"),
        @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CreateQueueResult createQueue(@Valid @RequestBody CreateQueueRequest request) {
        return promptQueueApplicationService.createQueue(
            new CreateQueueCommand(request.projectId(), request.name(), request.executionPolicy().toDomain())
        );
    }

    @GetMapping("/{queueId}")
    @Operation(summary = "Get queue details", description = "Returns queue details by id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Queue found"),
        @ApiResponse(responseCode = "400", description = "Invalid queue id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public QueueDetails getQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        return promptQueueApplicationService.getQueue(queueId);
    }

    @GetMapping
    @Operation(summary = "List project queues", description = "Returns queues that belong to a project.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Queues returned"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid project id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<QueueSummary> listProjectQueues(
        @Parameter(description = "Project id", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
        @RequestParam UUID projectId
    ) {
        return promptQueueApplicationService.listProjectQueues(projectId);
    }

    @PostMapping("/{queueId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Start queue", description = "Starts queue execution lifecycle.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Queue started", content = @Content),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow starting",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void startQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        promptQueueApplicationService.startQueue(queueId);
    }

    @PostMapping("/{queueId}/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pause queue", description = "Pauses a running queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Queue paused", content = @Content),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow pausing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void pauseQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        promptQueueApplicationService.pauseQueue(queueId);
    }

    @PostMapping("/{queueId}/resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Resume queue", description = "Resumes a paused or stopped queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Queue resumed", content = @Content),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow resuming",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void resumeQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        promptQueueApplicationService.resumeQueue(queueId);
    }

    @PostMapping("/{queueId}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Stop queue", description = "Stops queue execution. Request body is optional.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Queue stopped", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid stop reason",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow stopping",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void stopQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId,
        @Valid @RequestBody(required = false) StopQueueRequest request
    ) {
        promptQueueApplicationService.stopQueue(queueId, request == null ? null : request.reason());
    }

    @PostMapping("/{queueId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disable queue", description = "Disables a queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Queue disabled", content = @Content),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void disableQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        promptQueueApplicationService.disableQueue(queueId);
    }

    @PostMapping("/{queueId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Enable queue", description = "Enables a disabled queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Queue enabled", content = @Content),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow enabling",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void enableQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        promptQueueApplicationService.enableQueue(queueId);
    }

    @PatchMapping("/{queueId}/policy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change queue execution policy", description = "Replaces queue execution policy.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Execution policy changed", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid policy request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void changeExecutionPolicy(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId,
        @Valid @RequestBody ChangeQueuePolicyRequest request
    ) {
        promptQueueApplicationService.changeExecutionPolicy(
            queueId,
            new ChangeQueuePolicyCommand(request.executionPolicy().toDomain())
        );
    }
}
