package com.aiq.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiq.application.port.out.AiToolRepository;
import com.aiq.application.port.out.PromptExecutionRepository;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.port.out.PromptRepository;
import com.aiq.application.prompt.AddPromptCommand;
import com.aiq.application.prompt.dto.AddPromptResult;
import com.aiq.application.prompt.dto.PromptDetails;
import com.aiq.application.prompt.dto.PromptSummary;
import com.aiq.application.prompt.mapper.PromptMapper;
import com.aiq.domain.aitool.AiTool;
import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptOrderingService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PromptApplicationService {
    private final PromptRepository promptRepository;
    private final PromptQueueRepository promptQueueRepository;
    private final AiToolRepository aiToolRepository;
    private final PromptExecutionRepository promptExecutionRepository;
    private final PromptOrderingService promptOrderingService = new PromptOrderingService();

    public AddPromptResult addPrompt(AddPromptCommand command) {
        Objects.requireNonNull(command, "Add prompt command must not be null");
        ensureQueueExists(command.queueId());
        ensureAiToolEnabled(command.targetAiToolId());

        long nextPosition = promptRepository.nextPosition(command.queueId());
        Prompt prompt = Prompt.createQueued(
            command.queueId(),
            command.targetAiToolId(),
            command.title(),
            command.content(),
            command.priority(),
            nextPosition,
            command.maxAttempts(),
            command.workingDirectoryOverride()
        );

        Prompt savedPrompt = promptRepository.save(prompt);
        return PromptMapper.toAddPromptResult(savedPrompt);
    }

    public AddPromptResult addDraftPrompt(AddPromptCommand command) {
        Objects.requireNonNull(command, "Add prompt command must not be null");
        ensureQueueExists(command.queueId());
        ensureAiToolEnabled(command.targetAiToolId());

        long nextPosition = promptRepository.nextPosition(command.queueId());
        Prompt prompt = Prompt.createDraft(
            command.queueId(),
            command.targetAiToolId(),
            command.title(),
            command.content(),
            command.priority(),
            nextPosition,
            command.maxAttempts(),
            command.workingDirectoryOverride()
        );

        Prompt savedPrompt = promptRepository.save(prompt);
        return PromptMapper.toAddPromptResult(savedPrompt);
    }

    @Transactional(readOnly = true)
    public PromptDetails getPrompt(UUID promptId) {
        Prompt prompt = findPromptRequired(promptId);
        return PromptMapper.toDetails(prompt, promptExecutionRepository.findLatestByPromptId(prompt.getId()));
    }

    @Transactional(readOnly = true)
    public List<PromptSummary> listQueuePrompts(UUID queueId) {
        Objects.requireNonNull(queueId, "Queue id must not be null");
        ensureQueueExists(queueId);
        return promptOrderingService.order(promptRepository.findByQueueId(queueId)).stream()
            .map(PromptMapper::toSummary)
            .toList();
    }

    public void enqueuePrompt(UUID promptId) {
        Prompt prompt = findPromptRequired(promptId);
        prompt.enqueue();
        promptRepository.save(prompt);
    }

    public void changePromptTitle(UUID promptId, String title) {
        Prompt prompt = findPromptRequired(promptId);
        prompt.changeTitle(title);
        promptRepository.save(prompt);
    }

    public void changePromptContent(UUID promptId, String content) {
        Prompt prompt = findPromptRequired(promptId);
        prompt.changeContent(content);
        promptRepository.save(prompt);
    }

    public void changePromptPriority(UUID promptId, int priority) {
        Prompt prompt = findPromptRequired(promptId);
        prompt.changePriority(priority);
        promptRepository.save(prompt);
    }

    public void cancelPrompt(UUID promptId) {
        Prompt prompt = findPromptRequired(promptId);
        prompt.cancel();
        promptRepository.save(prompt);
    }

    public void skipPrompt(UUID promptId) {
        Prompt prompt = findPromptRequired(promptId);
        prompt.skip();
        promptRepository.save(prompt);
    }

    private Prompt findPromptRequired(UUID promptId) {
        Objects.requireNonNull(promptId, "Prompt id must not be null");
        return promptRepository.findById(promptId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt not found: " + promptId));
    }

    private void ensureQueueExists(UUID queueId) {
        Objects.requireNonNull(queueId, "Queue id must not be null");
        promptQueueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt queue not found: " + queueId));
    }

    private void ensureAiToolEnabled(UUID aiToolId) {
        Objects.requireNonNull(aiToolId, "AI tool id must not be null");
        AiTool aiTool = aiToolRepository.findById(aiToolId)
            .orElseThrow(() -> new IllegalArgumentException("AI tool not found: " + aiToolId));
        if (!aiTool.isEnabled()) {
            throw new IllegalArgumentException("AI tool is disabled: " + aiToolId);
        }
    }
}
