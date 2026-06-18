package com.aiq.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.application.port.out.AiLimitChecker;
import com.aiq.application.port.out.AiToolRepository;
import com.aiq.application.port.out.PromptExecutionRepository;
import com.aiq.application.port.out.PromptExecutor;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.port.out.PromptRepository;
import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.application.runner.RunQueueCommand;
import com.aiq.application.runner.dto.RunNextPromptResult;
import com.aiq.application.runner.dto.RunQueueResult;
import com.aiq.domain.aitool.AiTool;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.domain.queue.NextPromptSelector;
import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueStatus;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class QueueRunnerApplicationService {

    private final PromptQueueRepository promptQueueRepository;
    private final PromptRepository promptRepository;
    private final AiToolRepository aiToolRepository;
    private final PromptExecutionRepository promptExecutionRepository;
    private final AiLimitChecker aiLimitChecker;
    private final PromptExecutor promptExecutor;
    private final NextPromptSelector nextPromptSelector = new NextPromptSelector();

    public RunQueueResult runQueue(RunQueueCommand command) {
        Objects.requireNonNull(command, "Run queue command must not be null");
        if (command.maxPrompts() <= 0) {
            throw new IllegalArgumentException("Max prompts must be positive");
        }

        PromptQueue queue = findQueueRequired(command.queueId());
        int maxPrompts = Math.min(command.maxPrompts(), queue.getExecutionPolicy().maxPromptsPerRun());
        int executedPromptsCount = 0;

        String reason = "Run limit reached";
        boolean stoppedOnError = false;

        while (executedPromptsCount < maxPrompts) {
            RunNextPromptResult result = runNextPrompt(queue.getId());
            if (!result.executed()) {
                reason = result.reason();
                break;
            }

            executedPromptsCount++;
            reason = result.reason();

            PromptQueue currentQueue = findQueueRequired(queue.getId());
            if (currentQueue.getStatus() == QueueStatus.COMPLETED) {
                reason = "Queue completed";
                break;
            }
            if (currentQueue.getStatus() == QueueStatus.STOPPED) {
                stoppedOnError = currentQueue.shouldStopOnError();
                break;
            }
        }
        if (executedPromptsCount >= maxPrompts) {
            reason = finishQueueAfterRunLimit(queue.getId(), reason);
        }

        return new RunQueueResult(queue.getId(), executedPromptsCount, stoppedOnError, reason);
    }

    public RunNextPromptResult runNextPrompt(UUID queueId) {
        PromptQueue queue = findQueueRequired(queueId);
        if (!canExecute(queue)) {
            return new RunNextPromptResult(queue.getId(), null, false, "Queue cannot run from " + queue.getStatus());
        }

        List<Prompt> prompts = promptRepository.findByQueueId(queue.getId());
        return nextPromptSelector.selectNext(prompts)
            .map(prompt -> executePrompt(queue, prompt))
            .orElseGet(() -> completeQueue(queue));
    }

    private RunNextPromptResult executePrompt(PromptQueue queue, Prompt prompt) {
        AiTool aiTool = findEnabledAiToolRequired(prompt.getTargetAiToolId());
        AiLimitCheckResult limitCheckResult = checkLimit(prompt, aiTool);
        if (!limitCheckResult.isAvailable()) {
            return waitForLimit(queue, prompt, limitCheckResult);
        }

        startQueueIfNeeded(queue);

        PromptExecutionRequest request = new PromptExecutionRequest(
            prompt.getId(),
            prompt.getTargetAiToolId(),
            prompt.getTitle(),
            prompt.getContent(),
            prompt.workingDirectoryOverride().orElse(null)
        );
        PromptExecution execution = PromptExecution.create(
            prompt.getId(),
            prompt.getTargetAiToolId(),
            promptExecutor.buildCommand(request)
        );

        prompt.start();
        execution.start();

        ExecutionResult executionResult = execute(request);
        execution.complete(executionResult);

        if (executionResult.isSuccessful()) {
            prompt.complete();
        } else {
            failPrompt(queue, prompt, executionResult);
        }

        promptExecutionRepository.save(execution);
        promptRepository.save(prompt);

        if (executionResult.isSuccessful() && !hasQueuedPrompts(queue.getId())) {
            queue.complete();
            promptQueueRepository.save(queue);
        }

        return new RunNextPromptResult(
            queue.getId(),
            prompt.getId(),
            true,
            executionResult.isSuccessful() ? "Prompt executed successfully" : failureReason(executionResult)
        );
    }

    private AiLimitCheckResult checkLimit(Prompt prompt, AiTool aiTool) {
        AiLimitCheckRequest request = new AiLimitCheckRequest(
            aiTool.getId(),
            aiTool.getType(),
            aiTool.getExecutablePath(),
            prompt.workingDirectoryOverride().orElse(null)
        );

        try {
            AiLimitCheckResult result = aiLimitChecker.checkLimit(request);
            return result == null ? AiLimitCheckResult.error("AI limit checker returned null") : result;
        } catch (RuntimeException exception) {
            String reason = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
            return AiLimitCheckResult.error(reason);
        }
    }

    private RunNextPromptResult waitForLimit(PromptQueue queue, Prompt prompt, AiLimitCheckResult limitCheckResult) {
        queue.markWaitingLimit();
        promptQueueRepository.save(queue);

        return new RunNextPromptResult(
            queue.getId(),
            prompt.getId(),
            false,
            limitCheckResult.reason()
        );
    }

    private ExecutionResult execute(PromptExecutionRequest request) {
        try {
            return promptExecutor.execute(request);
        } catch (RuntimeException exception) {
            return new ExecutionResult(
                1,
                "",
                "",
                "",
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
            );
        }
    }

    private void failPrompt(PromptQueue queue, Prompt prompt, ExecutionResult executionResult) {
        String reason = failureReason(executionResult);
        prompt.fail(reason);

        if (queue.shouldStopOnError()) {
            queue.stop(reason);
            promptQueueRepository.save(queue);
            return;
        }

        if (prompt.canRetry()) {
            prompt.retry();
        }
    }

    private RunNextPromptResult completeQueue(PromptQueue queue) {
        if (queue.getStatus() != QueueStatus.COMPLETED) {
            queue.complete();
            promptQueueRepository.save(queue);
        }

        return new RunNextPromptResult(queue.getId(), null, false, "No queued prompts");
    }

    private void startQueueIfNeeded(PromptQueue queue) {
        if (queue.getStatus() != QueueStatus.RUNNING) {
            queue.start();
            promptQueueRepository.save(queue);
        }
    }

    private boolean canExecute(PromptQueue queue) {
        return queue.getStatus() == QueueStatus.RUNNING || queue.canRun();
    }

    private boolean hasQueuedPrompts(UUID queueId) {
        return nextPromptSelector.selectNext(promptRepository.findByQueueId(queueId)).isPresent();
    }

    private String finishQueueAfterRunLimit(UUID queueId, String currentReason) {
        PromptQueue queue = findQueueRequired(queueId);
        if (queue.getStatus() != QueueStatus.RUNNING) {
            return currentReason;
        }

        if (!hasQueuedPrompts(queueId)) {
            queue.complete();
            promptQueueRepository.save(queue);
            return "Queue completed";
        }

        queue.stop("Run limit reached");
        promptQueueRepository.save(queue);
        return "Run limit reached";
    }

    private String failureReason(ExecutionResult executionResult) {
        if (executionResult.errorMessage() != null && !executionResult.errorMessage().isBlank()) {
            return executionResult.errorMessage().trim();
        }
        if (!executionResult.stderr().isBlank()) {
            return executionResult.stderr().trim();
        }

        return "Prompt execution failed with exit code " + executionResult.exitCode();
    }

    private PromptQueue findQueueRequired(UUID queueId) {
        Objects.requireNonNull(queueId, "Queue id must not be null");
        return promptQueueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt queue not found: " + queueId));
    }

    private AiTool findEnabledAiToolRequired(UUID aiToolId) {
        Objects.requireNonNull(aiToolId, "AI tool id must not be null");
        AiTool aiTool = aiToolRepository.findById(aiToolId)
            .orElseThrow(() -> new IllegalArgumentException("AI tool not found: " + aiToolId));
        if (!aiTool.isEnabled()) {
            throw new IllegalArgumentException("AI tool is disabled: " + aiToolId);
        }

        return aiTool;
    }
}
