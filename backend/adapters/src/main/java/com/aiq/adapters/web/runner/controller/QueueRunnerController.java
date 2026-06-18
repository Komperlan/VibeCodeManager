package com.aiq.adapters.web.runner.controller;

import com.aiq.adapters.web.common.ErrorResponse;
import com.aiq.adapters.web.runner.request.RunQueueRequest;
import com.aiq.application.runner.RunQueueCommand;
import com.aiq.application.runner.dto.RunNextPromptResult;
import com.aiq.application.runner.dto.RunQueueResult;
import com.aiq.application.service.QueueRunnerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queues/{queueId}/runner")
@RequiredArgsConstructor
@Tag(name = "Queue Runner", description = "Manual queue execution operations")
public class QueueRunnerController {

    private final QueueRunnerApplicationService queueRunnerApplicationService;

    @PostMapping("/run")
    @Operation(summary = "Run queue", description = "Runs up to the requested number of prompts from a queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Queue run completed or stopped"),
        @ApiResponse(responseCode = "400", description = "Invalid run request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow running",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RunQueueResult runQueue(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId,
        @Valid @RequestBody RunQueueRequest request
    ) {
        return queueRunnerApplicationService.runQueue(
            new RunQueueCommand(queueId, request.maxPrompts())
        );
    }

    @PostMapping("/run-next")
    @Operation(summary = "Run next prompt", description = "Runs the next eligible prompt from a queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Next prompt run attempted"),
        @ApiResponse(responseCode = "400", description = "Invalid queue id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Queue not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Queue state does not allow running",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RunNextPromptResult runNextPrompt(
        @Parameter(description = "Queue id", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
        @PathVariable UUID queueId
    ) {
        return queueRunnerApplicationService.runNextPrompt(queueId);
    }
}
