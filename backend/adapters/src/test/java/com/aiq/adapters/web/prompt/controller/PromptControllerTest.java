package com.aiq.adapters.web.prompt.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.aiq.application.prompt.AddPromptCommand;
import com.aiq.application.prompt.dto.AddPromptResult;
import com.aiq.application.prompt.dto.PromptSummary;
import com.aiq.application.service.PromptApplicationService;
import com.aiq.domain.queue.PromptStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

class PromptControllerTest extends ControllerTestSupport {

    private final PromptApplicationService promptApplicationService = mock(PromptApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new PromptController(promptApplicationService));
    }

    @Test
    void shouldAddPrompt() throws Exception {
        UUID queueId = UUID.randomUUID();
        UUID aiToolId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();
        when(promptApplicationService.addPrompt(any(AddPromptCommand.class)))
            .thenReturn(new AddPromptResult(promptId));

        mockMvc.perform(post("/api/v1/prompts")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "queueId", queueId,
                    "targetAiToolId", aiToolId,
                    "title", "Fix tests",
                    "content", "Run tests and fix failures",
                    "priority", 7,
                    "maxAttempts", 2,
                    "workingDirectoryOverride", "/workspace/app"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.promptId").value(promptId.toString()));

        ArgumentCaptor<AddPromptCommand> captor = ArgumentCaptor.forClass(AddPromptCommand.class);
        verify(promptApplicationService).addPrompt(captor.capture());
        AddPromptCommand command = captor.getValue();
        Assertions.assertThat(command.queueId()).isEqualTo(queueId);
        Assertions.assertThat(command.targetAiToolId()).isEqualTo(aiToolId);
        Assertions.assertThat(command.title()).isEqualTo("Fix tests");
        Assertions.assertThat(command.content()).isEqualTo("Run tests and fix failures");
        Assertions.assertThat(command.priority()).isEqualTo(7);
        Assertions.assertThat(command.maxAttempts()).isEqualTo(2);
        Assertions.assertThat(command.workingDirectoryOverride()).isEqualTo("/workspace/app");
    }

    @Test
    void shouldRejectInvalidPromptRequest() throws Exception {
        mockMvc.perform(post("/api/v1/prompts")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "queueId", UUID.randomUUID(),
                    "targetAiToolId", UUID.randomUUID(),
                    "title", "   ",
                    "content", "   ",
                    "priority", -1,
                    "maxAttempts", 0
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.title[0]").value("title must not be blank"))
            .andExpect(jsonPath("$.details.content[0]").value("content must not be blank"))
            .andExpect(jsonPath("$.details.priority[0]").value("priority must be positive or 0"))
            .andExpect(jsonPath("$.details.maxAttempts[0]").value("maxAttempts must be positive"));

        verifyNoInteractions(promptApplicationService);
    }

    @Test
    void shouldListQueuePrompts() throws Exception {
        UUID queueId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();
        when(promptApplicationService.listQueuePrompts(queueId)).thenReturn(List.of(
            new PromptSummary(promptId, "First prompt", PromptStatus.QUEUED, 3, 11)
        ));

        mockMvc.perform(get("/api/v1/prompts").param("queueId", queueId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(promptId.toString()))
            .andExpect(jsonPath("$[0].title").value("First prompt"))
            .andExpect(jsonPath("$[0].status").value("QUEUED"))
            .andExpect(jsonPath("$[0].priority").value(3))
            .andExpect(jsonPath("$[0].position").value(11));
    }

    @Test
    void shouldReturnBadRequestWhenQueueIdParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/prompts"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MISSING_REQUEST_PARAMETER"))
            .andExpect(jsonPath("$.details.queueId[0]").value("must be present"));
    }

    @Test
    void shouldChangePromptContent() throws Exception {
        UUID promptId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/prompts/{promptId}/content", promptId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("content", "Updated content"))))
            .andExpect(status().isNoContent());

        verify(promptApplicationService).changePromptContent(promptId, "Updated content");
    }

    @Test
    void shouldChangePromptPosition() throws Exception {
        UUID promptId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/prompts/{promptId}/position", promptId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("position", 2))))
            .andExpect(status().isNoContent());

        verify(promptApplicationService).changePromptPosition(promptId, 2);
    }
}
