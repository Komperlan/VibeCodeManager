package com.aiq.adapters.web.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aiq.adapters.web.support.ControllerTestSupport;
import com.aiq.application.project.CreateProjectCommand;
import com.aiq.application.project.dto.CreateProjectResult;
import com.aiq.application.service.ProjectApplicationService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

class ProjectControllerTest extends ControllerTestSupport {

    private final ProjectApplicationService projectApplicationService = mock(ProjectApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new ProjectController(projectApplicationService));
    }

    @Test
    void shouldCreateProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectApplicationService.createProject(any(CreateProjectCommand.class)))
            .thenReturn(new CreateProjectResult(projectId));

        mockMvc.perform(post("/api/v1/projects")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "name", "Backend",
                    "rootDirectory", "/workspace/backend",
                    "codexSessionId", "019edddb-7d00-7df2-8577-d74b168adfad"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.projectId").value(projectId.toString()));

        ArgumentCaptor<CreateProjectCommand> captor = ArgumentCaptor.forClass(CreateProjectCommand.class);
        verify(projectApplicationService).createProject(captor.capture());
        CreateProjectCommand command = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.name()).isEqualTo("Backend");
        org.assertj.core.api.Assertions.assertThat(command.rootDirectory()).isEqualTo("/workspace/backend");
        org.assertj.core.api.Assertions.assertThat(command.codexSessionId()).isEqualTo("019edddb-7d00-7df2-8577-d74b168adfad");
    }

    @Test
    void shouldRejectBlankProjectName() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "name", "   ",
                    "rootDirectory", "/workspace/backend"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.name[0]").value("Project name must not be blank"));

        verifyNoInteractions(projectApplicationService);
    }

    @Test
    void shouldReturnNotFoundWhenProjectDoesNotExist() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectApplicationService.getProject(projectId))
            .thenThrow(new IllegalArgumentException("Project not found: " + projectId));

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Project not found: " + projectId));
    }

    @Test
    void shouldRenameProject() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/projects/{projectId}/name", projectId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("name", "New name"))))
            .andExpect(status().isNoContent());

        verify(projectApplicationService).renameProject(projectId, "New name");
    }

    @Test
    void shouldChangeProjectCodexSession() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/projects/{projectId}/codex-session", projectId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("codexSessionId", "019edddb-7d00-7df2-8577-d74b168adfad"))))
            .andExpect(status().isNoContent());

        verify(projectApplicationService)
            .changeCodexSession(projectId, "019edddb-7d00-7df2-8577-d74b168adfad");
    }

    @Test
    void shouldClearProjectCodexSession() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/projects/{projectId}/codex-session", projectId)
                .contentType(APPLICATION_JSON)
                .content("{\"codexSessionId\":null}"))
            .andExpect(status().isNoContent());

        verify(projectApplicationService).changeCodexSession(projectId, null);
    }

    @Test
    void shouldReturnConflictWhenProjectStateRejectsOperation() throws Exception {
        UUID projectId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new IllegalStateException("Archived project cannot be changed"))
            .when(projectApplicationService)
            .disableProject(projectId);

        mockMvc.perform(post("/api/v1/projects/{projectId}/disable", projectId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("Archived project cannot be changed"));

        verify(projectApplicationService, never()).archiveProject(projectId);
    }
}
