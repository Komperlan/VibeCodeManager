package com.aiq.adapters.web.runner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aiq.adapters.web.support.ControllerTestSupport;
import com.aiq.application.runner.RunQueueCommand;
import com.aiq.application.runner.dto.RunNextPromptResult;
import com.aiq.application.runner.dto.RunQueueResult;
import com.aiq.application.service.QueueRunnerApplicationService;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

class QueueRunnerControllerTest extends ControllerTestSupport {

    private final QueueRunnerApplicationService queueRunnerApplicationService =
        mock(QueueRunnerApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new QueueRunnerController(queueRunnerApplicationService));
    }

    @Test
    void shouldRunQueue() throws Exception {
        UUID queueId = UUID.randomUUID();
        when(queueRunnerApplicationService.runQueue(any(RunQueueCommand.class)))
            .thenReturn(new RunQueueResult(queueId, 2, false, "Run limit reached"));

        mockMvc.perform(post("/api/v1/queues/{queueId}/runner/run", queueId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("maxPrompts", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queueId").value(queueId.toString()))
            .andExpect(jsonPath("$.executedPrompts").value(2))
            .andExpect(jsonPath("$.stoppedOnError").value(false))
            .andExpect(jsonPath("$.reason").value("Run limit reached"));

        ArgumentCaptor<RunQueueCommand> captor = ArgumentCaptor.forClass(RunQueueCommand.class);
        verify(queueRunnerApplicationService).runQueue(captor.capture());
        Assertions.assertThat(captor.getValue().queueId()).isEqualTo(queueId);
        Assertions.assertThat(captor.getValue().maxPrompts()).isEqualTo(2);
    }

    @Test
    void shouldRejectNonPositiveRunLimit() throws Exception {
        UUID queueId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/queues/{queueId}/runner/run", queueId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("maxPrompts", 0))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.maxPrompts[0]").value("maxPrompts must be positive"));

        verifyNoInteractions(queueRunnerApplicationService);
    }

    @Test
    void shouldRunNextPrompt() throws Exception {
        UUID queueId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();
        when(queueRunnerApplicationService.runNextPrompt(queueId))
            .thenReturn(new RunNextPromptResult(queueId, promptId, true, "Prompt executed successfully"));

        mockMvc.perform(post("/api/v1/queues/{queueId}/runner/run-next", queueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queueId").value(queueId.toString()))
            .andExpect(jsonPath("$.promptId").value(promptId.toString()))
            .andExpect(jsonPath("$.executed").value(true))
            .andExpect(jsonPath("$.reason").value("Prompt executed successfully"));

        verify(queueRunnerApplicationService).runNextPrompt(queueId);
    }
}
