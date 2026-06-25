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
import com.aiq.application.project.CreateProjectCommand;
import com.aiq.application.project.dto.CreateProjectResult;
import com.aiq.application.project.dto.ProjectDetails;
import com.aiq.application.project.dto.ProjectSummary;
import com.aiq.domain.project.Project;
import com.aiq.domain.project.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProjectApplicationServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectApplicationService service = new ProjectApplicationService(projectRepository);

    @Test
    void shouldCreateProjectWhenRootDirectoryIsUnique() {
        CreateProjectCommand command = new CreateProjectCommand(" Backend ", "/workspace/backend");
        when(projectRepository.existsByRootDirectory(command.rootDirectory())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateProjectResult result = service.createProject(command);

        assertThat(result.projectId()).isNotNull();

        Project savedProject = savedProject();
        assertThat(savedProject.getId()).isEqualTo(result.projectId());
        assertThat(savedProject.getName()).isEqualTo("Backend");
        assertThat(savedProject.getRootDirectory()).isEqualTo("/workspace/backend");
        assertThat(savedProject.getCodexSessionId()).isNull();
        assertThat(savedProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void shouldCreateProjectWithExistingCodexSession() {
        CreateProjectCommand command = new CreateProjectCommand(
            "Backend",
            "/workspace/backend",
            "019edddb-7d00-7df2-8577-d74b168adfad"
        );
        when(projectRepository.existsByRootDirectory(command.rootDirectory())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createProject(command);

        assertThat(savedProject().getCodexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");
    }

    @Test
    void shouldRejectDuplicateRootDirectory() {
        CreateProjectCommand command = new CreateProjectCommand("Backend", "/workspace/backend");
        when(projectRepository.existsByRootDirectory(command.rootDirectory())).thenReturn(true);

        assertThatThrownBy(() -> service.createProject(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Project with this root directory already exists");

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldReturnProjectDetails() {
        Project project = project();
        givenProject(project);

        ProjectDetails details = service.getProject(project.getId());

        assertThat(details.id()).isEqualTo(project.getId());
        assertThat(details.name()).isEqualTo(project.getName());
        assertThat(details.rootDirectory()).isEqualTo(project.getRootDirectory());
        assertThat(details.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void shouldListProjectSummaries() {
        Project first = Project.create("First", "/workspace/first");
        Project second = Project.create("Second", "/workspace/second");
        second.disable();
        when(projectRepository.findAll()).thenReturn(List.of(first, second));

        List<ProjectSummary> projects = service.listProjects();

        assertThat(projects)
            .extracting(ProjectSummary::id)
            .containsExactly(first.getId(), second.getId());
        assertThat(projects)
            .extracting(ProjectSummary::status)
            .containsExactly(ProjectStatus.ACTIVE, ProjectStatus.DISABLED);
    }

    @Test
    void shouldRenameProjectAndSaveIt() {
        Project project = project();
        givenProject(project);

        service.renameProject(project.getId(), "New name");

        assertThat(project.getName()).isEqualTo("New name");
        verify(projectRepository).save(project);
    }

    @Test
    void shouldChangeAndClearCodexSession() {
        Project project = project();
        givenProject(project);

        service.changeCodexSession(project.getId(), "019edddb-7d00-7df2-8577-d74b168adfad");
        assertThat(project.getCodexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");

        service.changeCodexSession(project.getId(), null);
        assertThat(project.getCodexSessionId()).isNull();

        verify(projectRepository, org.mockito.Mockito.times(2)).save(project);
    }

    @Test
    void shouldRejectMissingProject() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProject(projectId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Project not found: " + projectId);

        verify(projectRepository).findById(projectId);
        verifyNoInteractionsAfterFind();
    }

    private Project project() {
        return Project.create("Backend", "/workspace/backend");
    }

    private void givenProject(Project project) {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
    }

    private Project savedProject() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        return captor.getValue();
    }

    private void verifyNoInteractionsAfterFind() {
        verify(projectRepository, never()).save(any(Project.class));
        verify(projectRepository, never()).findAll();
        verify(projectRepository, never()).existsByRootDirectory(any());
    }
}
