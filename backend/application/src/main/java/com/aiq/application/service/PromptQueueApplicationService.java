package com.aiq.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiq.application.port.out.ProjectRepository;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.queue.ChangeQueuePolicyCommand;
import com.aiq.application.queue.CreateQueueCommand;
import com.aiq.application.queue.dto.CreateQueueResult;
import com.aiq.application.queue.dto.QueueDetails;
import com.aiq.application.queue.dto.QueueSummary;
import com.aiq.application.queue.mapper.QueueMapper;
import com.aiq.domain.project.Project;
import com.aiq.domain.queue.PromptQueue;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PromptQueueApplicationService {
    private final PromptQueueRepository promptQueueRepository;
    private final ProjectRepository projectRepository;

    public CreateQueueResult createQueue(CreateQueueCommand command) {
        Objects.requireNonNull(command, "Create queue command must not be null");
        Project project = findProjectRequired(command.projectId());
        var promptQueue = PromptQueue.create(project.getId(), command.name(), command.executionPolicy());
        var createdPromptQueue = promptQueueRepository.save(promptQueue);
        return QueueMapper.toCreateQueueResult(createdPromptQueue);
    }

    @Transactional(readOnly = true)
    public QueueDetails getQueue(UUID queueId) {
        return QueueMapper.toDetails(findQueueRequired(queueId));
    }

    @Transactional(readOnly = true)
    public List<QueueSummary> listProjectQueues(UUID projectId) {
        findProjectRequired(projectId);
        return promptQueueRepository.findByProjectId(projectId)
            .stream()
            .map(QueueMapper::toSummary)
            .toList();
    }

    public void startQueue(UUID queueId) {
        PromptQueue queue = findQueueRequired(queueId);
        queue.start();
        promptQueueRepository.save(queue);
    }

    public void pauseQueue(UUID queueId) {
        PromptQueue queue = findQueueRequired(queueId);
        queue.pause();
        promptQueueRepository.save(queue);
    }

    public void resumeQueue(UUID queueId) {
        PromptQueue queue = findQueueRequired(queueId);
        queue.resume();
        promptQueueRepository.save(queue);
    }

    public void stopQueue(UUID queueId, String reason) {
        PromptQueue queue = findQueueRequired(queueId);
        queue.stop(reason);
        promptQueueRepository.save(queue);
    }

    public void disableQueue(UUID queueId) {
        PromptQueue queue = findQueueRequired(queueId);
        queue.disable();
        promptQueueRepository.save(queue);
    }

    public void enableQueue(UUID queueId) {
        PromptQueue queue = findQueueRequired(queueId);
        queue.enable();
        promptQueueRepository.save(queue);
    }

    public void changeExecutionPolicy(UUID queueId, ChangeQueuePolicyCommand command) {
        Objects.requireNonNull(command, "ChangeQueuePolicyCommand must not be null");
        PromptQueue queue = findQueueRequired(queueId);
        queue.changeExecutionPolicy(command.executionPolicy());
        promptQueueRepository.save(queue);
    }

    private Project findProjectRequired(UUID projectId) {
        Objects.requireNonNull(projectId, "Project id must not be null");
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    private PromptQueue findQueueRequired(UUID queueId) {
        Objects.requireNonNull(queueId, "Queue id must not be null");
        return promptQueueRepository.findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt queue not found: " + queueId));
    }
}
