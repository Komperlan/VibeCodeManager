package com.aiq.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aiq.application.port.out.AiToolRepository;
import com.aiq.application.port.out.PromptExecutionRepository;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.port.out.PromptRepository;
import com.aiq.application.prompt.AddPromptCommand;
import com.aiq.application.prompt.dto.AddPromptResult;
import com.aiq.application.prompt.dto.PromptDetails;
import com.aiq.application.prompt.dto.PromptSummary;
import com.aiq.domain.aitool.AiTool;
import com.aiq.domain.aitool.AiToolStatus;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.PromptStatus;
import com.aiq.domain.queue.QueueExecutionPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PromptApplicationServiceTest {

    private final UUID aiToolId = UUID.randomUUID();
    private final PromptRepository promptRepository = mock(PromptRepository.class);
    private final PromptQueueRepository promptQueueRepository = mock(PromptQueueRepository.class);
    private final AiToolRepository aiToolRepository = mock(AiToolRepository.class);
    private final PromptExecutionRepository promptExecutionRepository = mock(PromptExecutionRepository.class);
    private final PromptApplicationService service = new PromptApplicationService(
        promptRepository,
        promptQueueRepository,
        aiToolRepository,
        promptExecutionRepository
    );

    @Test
    void shouldAddQueuedPromptAtNextPosition() {
        PromptQueue queue = queue();
        givenQueue(queue);
        givenAiTool();
        when(promptRepository.nextPosition(queue.getId())).thenReturn(11L);
        when(promptRepository.save(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddPromptCommand command = command(queue.getId(), "Fix build", 3);
        AddPromptResult result = service.addPrompt(command);

        assertThat(result.promptId()).isNotNull();

        Prompt savedPrompt = savedPrompt();
        assertThat(savedPrompt.getId()).isEqualTo(result.promptId());
        assertThat(savedPrompt.getQueueId()).isEqualTo(queue.getId());
        assertThat(savedPrompt.getTargetAiToolId()).isEqualTo(aiToolId);
        assertThat(savedPrompt.getTitle()).isEqualTo("Fix build");
        assertThat(savedPrompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(savedPrompt.getPriority()).isEqualTo(3);
        assertThat(savedPrompt.getPosition()).isEqualTo(11L);
        assertThat(savedPrompt.getMaxAttempts()).isEqualTo(2);
        assertThat(savedPrompt.workingDirectoryOverride()).contains("/workspace/backend");
    }

    @Test
    void shouldAddDraftPrompt() {
        PromptQueue queue = queue();
        givenQueue(queue);
        givenAiTool();
        when(promptRepository.nextPosition(queue.getId())).thenReturn(1L);
        when(promptRepository.save(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddPromptResult result = service.addDraftPrompt(command(queue.getId(), "Draft", 0));

        assertThat(result.promptId()).isNotNull();
        assertThat(savedPrompt().getStatus()).isEqualTo(PromptStatus.DRAFT);
    }

    @Test
    void shouldRejectMissingAiToolWhenAddingPrompt() {
        PromptQueue queue = queue();
        givenQueue(queue);
        when(aiToolRepository.findById(aiToolId)).thenReturn(Optional.empty());

        AddPromptCommand command = command(queue.getId(), "Fix build", 0);

        assertThatThrownBy(() -> service.addPrompt(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AI tool not found: " + aiToolId);

        verify(promptRepository, never()).nextPosition(queue.getId());
        verify(promptRepository, never()).save(any(Prompt.class));
    }

    @Test
    void shouldRejectDisabledAiToolWhenAddingPrompt() {
        PromptQueue queue = queue();
        givenQueue(queue);
        givenAiTool(AiToolStatus.DISABLED);

        AddPromptCommand command = command(queue.getId(), "Fix build", 0);

        assertThatThrownBy(() -> service.addPrompt(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AI tool is disabled: " + aiToolId);

        verify(promptRepository, never()).nextPosition(queue.getId());
        verify(promptRepository, never()).save(any(Prompt.class));
    }

    @Test
    void shouldRejectMissingQueueWhenAddingPrompt() {
        UUID queueId = UUID.randomUUID();
        when(promptQueueRepository.findById(queueId)).thenReturn(Optional.empty());

        AddPromptCommand command = command(queueId, "Fix build", 0);

        assertThatThrownBy(() -> service.addPrompt(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Prompt queue not found: " + queueId);

        verifyNoInteractions(aiToolRepository, promptRepository);
    }

    @Test
    void shouldListQueuePromptsInDomainOrder() {
        PromptQueue queue = queue();
        Prompt lowPriority = prompt(queue, "Low priority", 1, 0);
        Prompt highPriority = prompt(queue, "High priority", 10, 1);
        Prompt samePriorityEarlier = prompt(queue, "Earlier", 1, 2);
        givenQueue(queue);
        when(promptRepository.findByQueueId(queue.getId()))
            .thenReturn(List.of(lowPriority, highPriority, samePriorityEarlier));

        List<PromptSummary> prompts = service.listQueuePrompts(queue.getId());

        assertThat(prompts)
            .extracting(PromptSummary::id)
            .containsExactly(highPriority.getId(), lowPriority.getId(), samePriorityEarlier.getId());
    }

    @Test
    void shouldReturnLastExecutionResultWithPromptDetails() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Ask Codex", 0, 0);
        PromptExecution execution = PromptExecution.create(prompt.getId(), aiToolId, "codex exec -");
        execution.start();
        execution.complete(new ExecutionResult(
            0,
            "Codex response text",
            "",
            "{\"msg\":\"Codex response text\"}",
            null
        ));
        givenPrompt(prompt);
        when(promptExecutionRepository.findLatestByPromptId(prompt.getId())).thenReturn(Optional.of(execution));

        PromptDetails details = service.getPrompt(prompt.getId());

        assertThat(details.lastExecution()).isPresent();
        assertThat(details.lastExecution().orElseThrow().responseText()).contains("Codex response text");
        assertThat(details.lastExecution().orElseThrow().rawOutput()).contains("{\"msg\":\"Codex response text\"}");
    }

    @Test
    void shouldEnqueueDraftPromptAndSaveIt() {
        PromptQueue queue = queue();
        Prompt prompt = Prompt.createDraft(
            queue.getId(),
            aiToolId,
            "Draft",
            "Draft content",
            0,
            0,
            2,
            null
        );
        givenPrompt(prompt);

        service.enqueuePrompt(prompt.getId());

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        verify(promptRepository).save(prompt);
    }

    @Test
    void shouldCancelPromptAndSaveIt() {
        PromptQueue queue = queue();
        Prompt prompt = prompt(queue, "Prompt", 0, 0);
        givenPrompt(prompt);

        service.cancelPrompt(prompt.getId());

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.CANCELLED);
        verify(promptRepository).save(prompt);
    }

    private AddPromptCommand command(UUID queueId, String title, int priority) {
        return new AddPromptCommand(
            queueId,
            aiToolId,
            title,
            title + " content",
            priority,
            2,
            "/workspace/backend"
        );
    }

    private PromptQueue queue() {
        return PromptQueue.create(UUID.randomUUID(), "Backend queue", QueueExecutionPolicy.defaultPolicy());
    }

    private Prompt prompt(PromptQueue queue, String title, int priority, long position) {
        return Prompt.createQueued(
            queue.getId(),
            aiToolId,
            title,
            title + " content",
            priority,
            position,
            2,
            null
        );
    }

    private void givenQueue(PromptQueue queue) {
        when(promptQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
    }

    private void givenAiTool() {
        givenAiTool(AiToolStatus.ENABLED);
    }

    private void givenAiTool(AiToolStatus status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        AiTool aiTool = AiTool.restore(
            aiToolId,
            "Codex",
            status,
            AiToolType.CODEX,
            "/usr/local/bin/codex",
            now,
            now
        );
        when(aiToolRepository.findById(aiToolId)).thenReturn(Optional.of(aiTool));
    }

    private void givenPrompt(Prompt prompt) {
        when(promptRepository.findById(prompt.getId())).thenReturn(Optional.of(prompt));
    }

    private Prompt savedPrompt() {
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(promptRepository).save(captor.capture());
        return captor.getValue();
    }
}
