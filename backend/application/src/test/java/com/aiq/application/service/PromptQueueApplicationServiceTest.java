package com.aiq.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aiq.application.port.out.ProjectRepository;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.queue.ChangeQueuePolicyCommand;
import com.aiq.application.queue.CreateQueueCommand;
import com.aiq.application.queue.dto.CreateQueueResult;
import com.aiq.application.queue.dto.QueueSummary;
import com.aiq.domain.project.Project;
import com.aiq.domain.queue.AutoRunMode;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.queue.QueueStatus;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PromptQueueApplicationServiceTest {

    private final PromptQueueRepository promptQueueRepository = mock(PromptQueueRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final PromptQueueApplicationService service = new PromptQueueApplicationService(
        promptQueueRepository,
        projectRepository
    );

    @Test
    void shouldCreateQueueForExistingProject() {
        Project project = project();
        givenProject(project);
        when(promptQueueRepository.save(any(PromptQueue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateQueueCommand command = new CreateQueueCommand(project.getId(), " Main queue ", null);
        CreateQueueResult result = service.createQueue(command);

        assertThat(result.queueId()).isNotNull();

        PromptQueue savedQueue = savedQueue();
        assertThat(savedQueue.getId()).isEqualTo(result.queueId());
        assertThat(savedQueue.getProjectId()).isEqualTo(project.getId());
        assertThat(savedQueue.getName()).isEqualTo("Main queue");
        assertThat(savedQueue.getStatus()).isEqualTo(QueueStatus.CREATED);
        assertThat(savedQueue.getExecutionPolicy()).isEqualTo(QueueExecutionPolicy.defaultPolicy());
    }

    @Test
    void shouldRejectNullCreateQueueCommand() {
        assertThatThrownBy(() -> service.createQueue(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Create queue command must not be null");

        verifyNoInteractions(promptQueueRepository, projectRepository);
    }

    @Test
    void shouldRejectMissingProjectWhenCreatingQueue() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        CreateQueueCommand command = new CreateQueueCommand(projectId, "Main queue", null);

        assertThatThrownBy(() -> service.createQueue(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Project not found: " + projectId);

        verify(promptQueueRepository, never()).save(any(PromptQueue.class));
    }

    @Test
    void shouldListQueuesOnlyForExistingProject() {
        Project project = project();
        PromptQueue first = queue(project, "First queue");
        PromptQueue second = queue(project, "Second queue");
        givenProject(project);
        when(promptQueueRepository.findByProjectId(project.getId())).thenReturn(List.of(first, second));

        List<QueueSummary> queues = service.listProjectQueues(project.getId());

        assertThat(queues)
            .extracting(QueueSummary::id)
            .containsExactly(first.getId(), second.getId());
        assertThat(queues)
            .extracting(QueueSummary::name)
            .containsExactly("First queue", "Second queue");
    }

    @Test
    void shouldRejectMissingProjectWhenListingQueues() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listProjectQueues(projectId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Project not found: " + projectId);

        verify(promptQueueRepository, never()).findByProjectId(projectId);
    }

    @Test
    void shouldStopQueueEvenWhenReasonIsNull() {
        Project project = project();
        PromptQueue queue = queue(project, "Main queue");
        queue.start();
        givenQueue(queue);

        service.stopQueue(queue.getId(), null);

        assertThat(queue.getStatus()).isEqualTo(QueueStatus.STOPPED);
        verify(promptQueueRepository).save(queue);
    }

    @Test
    void shouldChangeExecutionPolicyAndSaveQueue() {
        Project project = project();
        PromptQueue queue = queue(project, "Main queue");
        QueueExecutionPolicy policy = new QueueExecutionPolicy(
            AutoRunMode.AUTO_RUN,
            2,
            Duration.ofSeconds(5),
            false,
            false,
            null
        );
        givenQueue(queue);

        service.changeExecutionPolicy(queue.getId(), new ChangeQueuePolicyCommand(policy));

        assertThat(queue.getExecutionPolicy()).isEqualTo(policy);
        verify(promptQueueRepository).save(queue);
    }

    private Project project() {
        return Project.create("Backend", "/workspace/backend");
    }

    private PromptQueue queue(Project project, String name) {
        return PromptQueue.create(project.getId(), name, QueueExecutionPolicy.defaultPolicy());
    }

    private void givenProject(Project project) {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
    }

    private void givenQueue(PromptQueue queue) {
        when(promptQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
    }

    private PromptQueue savedQueue() {
        ArgumentCaptor<PromptQueue> captor = ArgumentCaptor.forClass(PromptQueue.class);
        verify(promptQueueRepository).save(captor.capture());
        return captor.getValue();
    }
}
