package com.aiq.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.application.port.out.AiLimitChecker;
import com.aiq.application.port.out.AiToolRepository;
import com.aiq.application.port.out.PromptExecutionRepository;
import com.aiq.application.port.out.PromptExecutor;
import com.aiq.application.port.out.ProjectRepository;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.port.out.PromptRepository;
import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.application.runner.RunQueueCommand;
import com.aiq.application.runner.dto.RunNextPromptResult;
import com.aiq.application.runner.dto.RunQueueResult;
import com.aiq.domain.aitool.AiTool;
import com.aiq.domain.aitool.AiToolStatus;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.domain.execution.ExecutionStatus;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.domain.project.Project;
import com.aiq.domain.queue.AutoRunMode;
import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.PromptStatus;
import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.queue.QueueStatus;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QueueRunnerApplicationServiceTest {

    private final UUID aiToolId = UUID.randomUUID();
    private static final String PROJECT_ROOT_DIRECTORY = "/workspace/project-root";
    private final PromptQueueRepository promptQueueRepository = mock(PromptQueueRepository.class);
    private final PromptRepository promptRepository = mock(PromptRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final AiToolRepository aiToolRepository = mock(AiToolRepository.class);
    private final PromptExecutionRepository promptExecutionRepository = mock(PromptExecutionRepository.class);
    private final AiLimitChecker aiLimitChecker = mock(AiLimitChecker.class);
    private final PromptExecutor promptExecutor = mock(PromptExecutor.class);

    private final QueueRunnerApplicationService service = new QueueRunnerApplicationService(
        promptQueueRepository,
        promptRepository,
        projectRepository,
        aiToolRepository,
        promptExecutionRepository,
        aiLimitChecker,
        promptExecutor
    );

    @Test
    void shouldExecuteSinglePromptAndCompleteQueue() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Fix tests", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenSuccessfulExecutor();

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isTrue();
        assertThat(result.promptId()).isEqualTo(prompt.getId());
        assertThat(result.reason()).isEqualTo("Prompt executed successfully");

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.COMPLETED);

        PromptExecution execution = savedExecution();
        assertThat(execution.getPromptId()).isEqualTo(prompt.getId());
        assertThat(execution.getAiToolId()).isEqualTo(aiToolId);
        assertThat(execution.getCommand()).isEqualTo("test-command");
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);

        verify(promptRepository).save(prompt);
        verify(promptQueueRepository, times(2)).save(queue);
    }

    @Test
    void shouldRunCompletedQueueWhenNewPromptWasAdded() {
        PromptQueue queue = queue();
        queue.complete();
        Prompt prompt = prompt(queue, "New work after completion", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenSuccessfulExecutor();

        RunQueueResult result = service.runQueue(new RunQueueCommand(queue.getId(), 3));

        assertThat(result.executedPrompts()).isEqualTo(1);
        assertThat(result.stoppedOnError()).isFalse();
        assertThat(result.reason()).isEqualTo("Queue completed");
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.COMPLETED);

        verify(promptExecutor).execute(any(PromptExecutionRequest.class));
        verify(promptRepository).save(prompt);
    }

    @Test
    void shouldMoveCompletedQueueToWaitingLimitWhenNewPromptCannotStartBecauseOfLimit() {
        PromptQueue queue = queue();
        queue.complete();
        Prompt prompt = prompt(queue, "Wait after completion", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenLimitResult(AiLimitCheckResult.limitReached("Codex usage limit reached", null));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isFalse();
        assertThat(result.promptId()).isEqualTo(prompt.getId());
        assertThat(result.reason()).isEqualTo("Codex usage limit reached");
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);

        verify(promptQueueRepository).save(queue);
        verifyNoInteractions(promptExecutionRepository, promptExecutor);
    }

    @Test
    void shouldExecuteEarliestPositionPromptFirst() {
        PromptQueue queue = queue();
        Prompt earliest = prompt(queue, "Earliest", 0, 0);
        Prompt laterHighPriority = prompt(queue, "Later high priority", 5, 1);
        Prompt latest = prompt(queue, "Latest", 0, 2);

        givenQueue(queue);
        givenPrompts(queue, earliest, laterHighPriority, latest);
        givenSuccessfulExecutor();

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.promptId()).isEqualTo(earliest.getId());
        assertThat(earliest.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(laterHighPriority.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(latest.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.RUNNING);

        PromptExecutionRequest request = executedRequest();
        assertThat(request.promptId()).isEqualTo(earliest.getId());
        assertThat(request.title()).isEqualTo(earliest.getTitle());

        verify(promptRepository).save(earliest);
        verify(promptQueueRepository).save(queue);
    }

    @Test
    void shouldUseProjectRootDirectoryWhenPromptHasNoWorkingDirectoryOverride() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Use project root", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenSuccessfulExecutor();

        service.runNextPrompt(queue.getId());

        PromptExecutionRequest executionRequest = executedRequest();
        assertThat(executionRequest.workingDirectoryOverride()).isEqualTo(PROJECT_ROOT_DIRECTORY);

        AiLimitCheckRequest limitRequest = limitCheckRequest();
        assertThat(limitRequest.workingDirectory()).contains(PROJECT_ROOT_DIRECTORY);
    }

    @Test
    void shouldPassExistingProjectCodexSessionToExecutor() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Continue project context", 0, 0);
        Project project = Project.create(
            "Backend",
            PROJECT_ROOT_DIRECTORY,
            "019edddb-7d00-7df2-8577-d74b168adfad"
        );

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenProject(queue, project);
        givenSuccessfulExecutor();

        service.runNextPrompt(queue.getId());

        PromptExecutionRequest request = executedRequest();
        assertThat(request.codexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldSaveNewCodexSessionReturnedByExecutor() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Create project context", 0, 0);
        Project project = Project.create("Backend", PROJECT_ROOT_DIRECTORY);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenProject(queue, project);
        givenExecutorResult(new ExecutionResult(
            0,
            "Execution finished",
            "",
            "Execution finished",
            null,
            "019edddb-7d00-7df2-8577-d74b168adfad"
        ));

        service.runNextPrompt(queue.getId());

        assertThat(project.getCodexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");
        verify(projectRepository).save(project);
    }

    @Test
    void shouldNotOverwriteExistingProjectCodexSession() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Keep chosen context", 0, 0);
        Project project = Project.create(
            "Backend",
            PROJECT_ROOT_DIRECTORY,
            "existing-session"
        );

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenProject(queue, project);
        givenExecutorResult(new ExecutionResult(
            0,
            "Execution finished",
            "",
            "Execution finished",
            null,
            "new-session"
        ));

        service.runNextPrompt(queue.getId());

        assertThat(project.getCodexSessionId()).isEqualTo("existing-session");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldExpandHomeDirectoryInProjectRootDirectory() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Use home shortcut", 0, 0);
        String expectedWorkingDirectory = java.nio.file.Path.of(
            System.getProperty("user.home"),
            "testDirForVCManager"
        ).toString();

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenProject(queue, "~/testDirForVCManager");
        givenSuccessfulExecutor();

        service.runNextPrompt(queue.getId());

        PromptExecutionRequest executionRequest = executedRequest();
        assertThat(executionRequest.workingDirectoryOverride()).isEqualTo(expectedWorkingDirectory);

        AiLimitCheckRequest limitRequest = limitCheckRequest();
        assertThat(limitRequest.workingDirectory()).isEqualTo(expectedWorkingDirectory);
    }

    @Test
    void shouldPreferPromptWorkingDirectoryOverrideOverProjectRoot() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Use override", 0, 0, "/tmp/prompt-workdir");

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenSuccessfulExecutor();

        service.runNextPrompt(queue.getId());

        PromptExecutionRequest executionRequest = executedRequest();
        assertThat(executionRequest.workingDirectoryOverride()).isEqualTo("/tmp/prompt-workdir");

        AiLimitCheckRequest limitRequest = limitCheckRequest();
        assertThat(limitRequest.workingDirectory()).contains("/tmp/prompt-workdir");
    }

    @Test
    void shouldCompleteQueueWhenThereAreNoQueuedPrompts() {
        PromptQueue queue = queue();

        givenQueue(queue);
        givenPrompts(queue);

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isFalse();
        assertThat(result.promptId()).isNull();
        assertThat(result.reason()).isEqualTo("No queued prompts");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.COMPLETED);

        verify(promptQueueRepository).save(queue);
        verifyNoInteractions(promptExecutor, promptExecutionRepository);
    }

    @Test
    void shouldNotRunPausedQueue() {
        PromptQueue queue = queue();
        queue.start();
        queue.pause();

        givenQueue(queue);

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isFalse();
        assertThat(result.promptId()).isNull();
        assertThat(result.reason()).isEqualTo("Queue cannot run from PAUSED");

        verify(promptQueueRepository, never()).save(any());
        verifyNoInteractions(promptRepository, promptExecutor, promptExecutionRepository);
    }

    @Test
    void shouldStopQueueWhenPromptFailsAndPolicyRequiresIt() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Failing prompt", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenFailedExecutor("Tool crashed");

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isTrue();
        assertThat(result.promptId()).isEqualTo(prompt.getId());
        assertThat(result.reason()).isEqualTo("Tool crashed");

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.FAILED);
        assertThat(prompt.failureReason()).contains("Tool crashed");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.STOPPED);

        assertThat(savedExecution().getStatus()).isEqualTo(ExecutionStatus.FAILED);
        verify(promptRepository).save(prompt);
        verify(promptQueueRepository, times(2)).save(queue);
    }

    @Test
    void shouldStopRunWhenCommandLimitIsReached() {
        PromptQueue queue = queue();
        Prompt first = prompt(queue, "First", 0, 0);
        Prompt second = prompt(queue, "Second", 0, 1);

        givenQueue(queue);
        givenPrompts(queue, first, second);
        givenSuccessfulExecutor();

        RunQueueResult result = service.runQueue(new RunQueueCommand(queue.getId(), 1));

        assertThat(result.queueId()).isEqualTo(queue.getId());
        assertThat(result.executedPrompts()).isEqualTo(1);
        assertThat(result.stoppedOnError()).isFalse();
        assertThat(result.reason()).isEqualTo("Run limit reached");

        assertThat(first.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.STOPPED);

        verify(promptExecutor, times(1)).execute(any(PromptExecutionRequest.class));
    }

    @Test
    void shouldRejectNonPositiveRunLimit() {
        RunQueueCommand command = new RunQueueCommand(UUID.randomUUID(), 0);

        assertThatThrownBy(() -> service.runQueue(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Max prompts must be positive");

        verifyNoInteractions(
            promptQueueRepository,
            promptRepository,
            promptExecutionRepository,
            promptExecutor
        );
    }

    @Test
    void shouldRejectNegativeRunLimit() {
        RunQueueCommand command = new RunQueueCommand(UUID.randomUUID(), -1);

        assertThatThrownBy(() -> service.runQueue(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Max prompts must be positive");

        verifyNoInteractions(
            promptQueueRepository,
            promptRepository,
            promptExecutionRepository,
            promptExecutor
        );
    }

    @Test
    void shouldRejectNullRunCommand() {
        assertThatThrownBy(() -> service.runQueue(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Run queue command must not be null");

        verifyNoInteractions(
            promptQueueRepository,
            promptRepository,
            promptExecutionRepository,
            promptExecutor
        );
    }

    @Test
    void shouldRejectNullQueueIdForNextPrompt() {
        assertThatThrownBy(() -> service.runNextPrompt(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Queue id must not be null");

        verifyNoInteractions(
            promptQueueRepository,
            promptRepository,
            promptExecutionRepository,
            promptExecutor
        );
    }

    @Test
    void shouldRejectMissingQueue() {
        UUID queueId = UUID.randomUUID();
        when(promptQueueRepository.findByIdForUpdate(queueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runNextPrompt(queueId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Prompt queue not found: " + queueId);

        verify(promptQueueRepository).findByIdForUpdate(queueId);
        verifyNoInteractions(promptRepository, promptExecutionRepository, promptExecutor);
    }

    @Test
    void shouldUsePositionWhenPrioritiesAreEqual() {
        PromptQueue queue = queue();
        Prompt second = prompt(queue, "Second", 1, 20);
        Prompt first = prompt(queue, "First", 1, 10);

        givenQueue(queue);
        givenPrompts(queue, second, first);
        givenSuccessfulExecutor();

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.promptId()).isEqualTo(first.getId());
        assertThat(first.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(PromptStatus.QUEUED);
    }

    @Test
    void shouldIgnorePromptsThatAreNotQueued() {
        PromptQueue queue = queue();
        Prompt completedHighPriority = prompt(queue, "Already done", 100, 0);
        completedHighPriority.start();
        completedHighPriority.complete();
        Prompt queuedLowPriority = prompt(queue, "Still queued", 1, 1);

        givenQueue(queue);
        givenPrompts(queue, completedHighPriority, queuedLowPriority);
        givenSuccessfulExecutor();

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.promptId()).isEqualTo(queuedLowPriority.getId());
        assertThat(completedHighPriority.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(queuedLowPriority.getStatus()).isEqualTo(PromptStatus.COMPLETED);
    }

    @Test
    void shouldRetryFailedPromptWhenQueueDoesNotStopOnError() {
        PromptQueue queue = queue(policy(3, false));
        Prompt prompt = prompt(queue, "Retry me", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenFailedExecutor("Temporary failure");

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isTrue();
        assertThat(result.reason()).isEqualTo("Temporary failure");
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(prompt.getAttemptCount()).isEqualTo(1);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.RUNNING);
        assertThat(savedExecution().getStatus()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void shouldSaveFailedExecutionWhenExecutorThrowsException() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Explodes", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        when(promptExecutor.buildCommand(any(PromptExecutionRequest.class))).thenReturn("test-command");
        when(promptExecutor.execute(any(PromptExecutionRequest.class))).thenThrow(new RuntimeException("Executor crashed"));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.reason()).isEqualTo("Executor crashed");
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.FAILED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.STOPPED);
        assertThat(savedExecution().getStatus()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void shouldUseStderrAsFailureReasonWhenErrorMessageIsBlank() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Fails with stderr", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenExecutorResult(new ExecutionResult(
            1,
            "",
            "stderr failure",
            "stderr failure",
            " "
        ));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.reason()).isEqualTo("stderr failure");
        assertThat(prompt.failureReason()).contains("stderr failure");
        assertThat(savedExecution().getStatus()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void shouldUseExitCodeAsFallbackFailureReason() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Fails without message", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenExecutorResult(new ExecutionResult(
            7,
            "",
            "",
            "",
            null
        ));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.reason()).isEqualTo("Prompt execution failed with exit code 7");
        assertThat(prompt.failureReason()).contains("Prompt execution failed with exit code 7");
        assertThat(savedExecution().getStatus()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void shouldRespectPolicyMaxPromptsPerRun() {
        PromptQueue queue = queue(policy(1, true));
        Prompt first = prompt(queue, "First", 0, 0);
        Prompt second = prompt(queue, "Second", 0, 1);

        givenQueue(queue);
        givenPrompts(queue, first, second);
        givenSuccessfulExecutor();

        RunQueueResult result = service.runQueue(new RunQueueCommand(queue.getId(), 5));

        assertThat(result.executedPrompts()).isEqualTo(1);
        assertThat(result.reason()).isEqualTo("Run limit reached");
        assertThat(first.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.STOPPED);
        verify(promptExecutor, times(1)).execute(any(PromptExecutionRequest.class));
    }

    @Test
    void shouldWaitForLimitWhenLimitIsReachedBeforeNextPrompt() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Wait for Codex", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenLimitResult(AiLimitCheckResult.limitReached("Codex usage limit reached", null));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isFalse();
        assertThat(result.promptId()).isEqualTo(prompt.getId());
        assertThat(result.reason()).isEqualTo("Codex usage limit reached");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);

        verify(promptQueueRepository).save(queue);
        verifyNoInteractions(promptExecutionRepository, promptExecutor);
        verify(promptRepository, never()).save(any());
    }

    @Test
    void shouldStopQueueWhenLimitCheckerFails() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Wait on checker error", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenLimitResult(AiLimitCheckResult.error("Codex limit probe failed"));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isFalse();
        assertThat(result.promptId()).isEqualTo(prompt.getId());
        assertThat(result.reason()).isEqualTo("AI limit check failed: Codex limit probe failed");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.STOPPED);
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);

        verify(promptQueueRepository).save(queue);
        verifyNoInteractions(promptExecutionRepository, promptExecutor);
    }

    @Test
    void shouldStopRunQueueWhenLimitIsReachedBeforeFirstPrompt() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Wait before first", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenLimitResult(AiLimitCheckResult.limitReached("Codex rate limit", null));

        RunQueueResult result = service.runQueue(new RunQueueCommand(queue.getId(), 3));

        assertThat(result.executedPrompts()).isZero();
        assertThat(result.stoppedOnError()).isFalse();
        assertThat(result.reason()).isEqualTo("Codex rate limit");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);

        verifyNoInteractions(promptExecutionRepository, promptExecutor);
    }

    @Test
    void shouldResumeQueueThatIsStillWaitingForLimit() {
        PromptQueue queue = queue();
        queue.markWaitingLimit();
        Prompt prompt = prompt(queue, "Resume after limit", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenSuccessfulExecutor();

        RunQueueResult result = service.resumeWaitingLimitQueue(queue.getId());

        assertThat(result.executedPrompts()).isEqualTo(1);
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.COMPLETED);
    }

    @Test
    void shouldNotAutomaticallyResumeQueueWhoseStatusHasChanged() {
        PromptQueue queue = queue();
        givenQueue(queue);

        RunQueueResult result = service.resumeWaitingLimitQueue(queue.getId());

        assertThat(result.executedPrompts()).isZero();
        assertThat(result.reason()).isEqualTo("Queue is no longer waiting for limit");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.CREATED);
        verifyNoInteractions(promptRepository, promptExecutionRepository, promptExecutor);
    }

    @Test
    void shouldStopRunQueueWhenLimitIsReachedAfterSuccessfulPrompt() {
        PromptQueue queue = queue(policy(3, true));
        Prompt first = prompt(queue, "First", 0, 0);
        Prompt second = prompt(queue, "Second", 0, 1);

        givenQueue(queue);
        givenPrompts(queue, first, second);
        givenLimitResults(
            AiLimitCheckResult.available(),
            AiLimitCheckResult.limitReached("Codex quota exhausted", null)
        );
        givenSuccessfulExecutor();

        RunQueueResult result = service.runQueue(new RunQueueCommand(queue.getId(), 3));

        assertThat(result.executedPrompts()).isEqualTo(1);
        assertThat(result.stoppedOnError()).isFalse();
        assertThat(result.reason()).isEqualTo("Codex quota exhausted");
        assertThat(first.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);

        verify(promptExecutor, times(1)).execute(any(PromptExecutionRequest.class));
    }

    @Test
    void shouldReturnPromptToQueueWhenExecutorReportsLimitReached() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Runtime limit", 0, 0);
        ExecutionResult executionResult = new ExecutionResult(
            1,
            "",
            "Codex usage limit reached",
            "Codex usage limit reached",
            "Codex usage limit reached"
        );

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenExecutorResult(executionResult);
        when(aiLimitChecker.detectLimit(any(AiLimitCheckRequest.class), any(ExecutionResult.class)))
            .thenReturn(Optional.of(AiLimitCheckResult.limitReached("Codex usage limit reached", null)));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("Codex usage limit reached");
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(prompt.getAttemptCount()).isZero();
        assertThat(savedExecution().getStatus()).isEqualTo(ExecutionStatus.FAILED);
        verify(promptRepository).save(prompt);
        verify(promptQueueRepository, times(2)).save(queue);
    }

    @Test
    void shouldNotInspectSuccessfulExecutorOutputForLimitPatterns() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Explain rate limits", 0, 0);

        givenQueue(queue);
        givenPrompts(queue, prompt);
        givenExecutorResult(new ExecutionResult(
            0,
            "A rate limit controls request frequency",
            "",
            "A rate limit controls request frequency",
            null
        ));

        RunNextPromptResult result = service.runNextPrompt(queue.getId());

        assertThat(result.executed()).isTrue();
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        verify(aiLimitChecker, never()).detectLimit(any(), any());
    }

    private PromptQueue queue() {
        return queue(QueueExecutionPolicy.defaultPolicy());
    }

    private PromptQueue queue(QueueExecutionPolicy policy) {
        return PromptQueue.create(
            UUID.randomUUID(),
            "Backend queue",
            policy
        );
    }

    private QueueExecutionPolicy policy(int maxPromptsPerRun, boolean stopOnError) {
        return new QueueExecutionPolicy(
            AutoRunMode.ASK_CONFIRMATION,
            maxPromptsPerRun,
            Duration.ZERO,
            stopOnError,
            false,
            null
        );
    }

    private Prompt prompt(PromptQueue queue, String title, int priority, long position) {
        return prompt(queue, title, priority, position, null);
    }

    private Prompt prompt(PromptQueue queue, String title, int priority, long position, String workingDirectoryOverride) {
        return Prompt.createQueued(
            queue.getId(),
            aiToolId,
            title,
            title + " content",
            priority,
            position,
            3,
            workingDirectoryOverride
        );
    }

    private void givenQueue(PromptQueue queue) {
        when(promptQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(promptQueueRepository.findByIdForUpdate(queue.getId())).thenReturn(Optional.of(queue));
    }

    private void givenPrompts(PromptQueue queue, Prompt... prompts) {
        List<Prompt> promptList = Arrays.asList(prompts);
        when(promptRepository.findByQueueId(queue.getId())).thenReturn(promptList);
        if (!promptList.isEmpty()) {
            givenProject(queue);
            givenAiToolEnabled(aiToolId);
            givenLimitResult(AiLimitCheckResult.available());
        }
    }

    private void givenProject(PromptQueue queue) {
        givenProject(queue, PROJECT_ROOT_DIRECTORY);
    }

    private void givenProject(PromptQueue queue, String rootDirectory) {
        givenProject(queue, Project.create("Backend", rootDirectory));
    }

    private void givenProject(PromptQueue queue, Project project) {
        when(projectRepository.findById(queue.getProjectId()))
            .thenReturn(Optional.of(project));
    }

    private void givenAiToolEnabled(UUID toolId) {
        when(aiToolRepository.findById(toolId)).thenReturn(Optional.of(aiTool(toolId, AiToolStatus.ENABLED)));
    }

    private void givenSuccessfulExecutor() {
        givenExecutorResult(new ExecutionResult(
            0,
            "Execution finished",
            "",
            "Execution finished",
            null
        ));
    }

    private void givenFailedExecutor(String reason) {
        givenExecutorResult(new ExecutionResult(
            1,
            "",
            reason,
            reason,
            reason
        ));
    }

    private void givenExecutorResult(ExecutionResult result) {
        when(promptExecutor.buildCommand(any(PromptExecutionRequest.class))).thenReturn("test-command");
        when(promptExecutor.execute(any(PromptExecutionRequest.class))).thenReturn(result);
    }

    private void givenLimitResult(AiLimitCheckResult result) {
        when(aiLimitChecker.checkLimit(any(AiLimitCheckRequest.class))).thenReturn(result);
    }

    private void givenLimitResults(AiLimitCheckResult first, AiLimitCheckResult second) {
        when(aiLimitChecker.checkLimit(any(AiLimitCheckRequest.class))).thenReturn(first, second);
    }

    private AiTool aiTool(UUID toolId, AiToolStatus status) {
        return AiTool.restore(
            toolId,
            "Codex",
            status,
            AiToolType.CODEX,
            "codex",
            java.time.Instant.now(),
            java.time.Instant.now()
        );
    }

    private PromptExecution savedExecution() {
        ArgumentCaptor<PromptExecution> captor = ArgumentCaptor.forClass(PromptExecution.class);
        verify(promptExecutionRepository).save(captor.capture());
        return captor.getValue();
    }

    private PromptExecutionRequest executedRequest() {
        ArgumentCaptor<PromptExecutionRequest> captor = ArgumentCaptor.forClass(PromptExecutionRequest.class);
        verify(promptExecutor).execute(captor.capture());
        return captor.getValue();
    }

    private AiLimitCheckRequest limitCheckRequest() {
        ArgumentCaptor<AiLimitCheckRequest> captor = ArgumentCaptor.forClass(AiLimitCheckRequest.class);
        verify(aiLimitChecker).checkLimit(captor.capture());
        return captor.getValue();
    }
}
