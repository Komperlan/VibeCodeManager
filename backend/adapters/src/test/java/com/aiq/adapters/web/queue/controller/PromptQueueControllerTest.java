package com.aiq.adapters.web.queue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aiq.adapters.web.support.ControllerTestSupport;
import com.aiq.application.queue.ChangeQueuePolicyCommand;
import com.aiq.application.queue.CreateQueueCommand;
import com.aiq.application.queue.dto.CreateQueueResult;
import com.aiq.application.service.PromptQueueApplicationService;
import com.aiq.domain.queue.AutoRunMode;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

class PromptQueueControllerTest extends ControllerTestSupport {

    private final PromptQueueApplicationService promptQueueApplicationService =
        mock(PromptQueueApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new PromptQueueController(promptQueueApplicationService));
    }

    @Test
    void shouldCreateQueue() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        when(promptQueueApplicationService.createQueue(any(CreateQueueCommand.class)))
            .thenReturn(new CreateQueueResult(queueId));

        mockMvc.perform(post("/api/v1/queues")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "projectId", projectId,
                    "name", "Main queue",
                    "executionPolicy", Map.of(
                        "autoRunMode", "ASK_CONFIRMATION",
                        "maxPromptsPerRun", 3,
                        "cooldown", "PT1M",
                        "stopOnError", true,
                        "workingHoursEnabled", false
                    )
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.queueId").value(queueId.toString()));

        ArgumentCaptor<CreateQueueCommand> captor = ArgumentCaptor.forClass(CreateQueueCommand.class);
        verify(promptQueueApplicationService).createQueue(captor.capture());
        CreateQueueCommand command = captor.getValue();
        Assertions.assertThat(command.projectId()).isEqualTo(projectId);
        Assertions.assertThat(command.name()).isEqualTo("Main queue");
        Assertions.assertThat(command.executionPolicy().autoRunMode()).isEqualTo(AutoRunMode.ASK_CONFIRMATION);
        Assertions.assertThat(command.executionPolicy().maxPromptsPerRun()).isEqualTo(3);
        Assertions.assertThat(command.executionPolicy().cooldown()).isEqualTo(Duration.ofMinutes(1));
        Assertions.assertThat(command.executionPolicy().stopOnError()).isTrue();
        Assertions.assertThat(command.executionPolicy().workingHoursEnabled()).isFalse();
        Assertions.assertThat(command.executionPolicy().workingHours()).isNull();
    }

    @Test
    void shouldRejectQueuePolicyWithoutWorkingHoursWhenTheyAreEnabled() throws Exception {
        mockMvc.perform(post("/api/v1/queues")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "projectId", UUID.randomUUID(),
                    "name", "Main queue",
                    "executionPolicy", Map.of(
                        "autoRunMode", "AUTO_RUN",
                        "maxPromptsPerRun", 3,
                        "cooldown", "PT1M",
                        "stopOnError", true,
                        "workingHoursEnabled", true
                    )
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details['executionPolicy.workingHoursValid'][0]")
                .value("workingHours must be set when working hours are enabled"));
    }

    @Test
    void shouldStopQueueWithoutRequestBody() throws Exception {
        UUID queueId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/queues/{queueId}/stop", queueId))
            .andExpect(status().isNoContent());

        verify(promptQueueApplicationService).stopQueue(queueId, null);
    }

    @Test
    void shouldRejectBlankStopReason() throws Exception {
        UUID queueId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/queues/{queueId}/stop", queueId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("reason", "   "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.reasonValid[0]").value("reason must not be blank"));
    }

    @Test
    void shouldChangeExecutionPolicy() throws Exception {
        UUID queueId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/queues/{queueId}/policy", queueId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "executionPolicy", Map.of(
                        "autoRunMode", "NOTIFY_ONLY",
                        "maxPromptsPerRun", 1,
                        "cooldown", "PT0S",
                        "stopOnError", false,
                        "workingHoursEnabled", false
                    )
                ))))
            .andExpect(status().isNoContent());

        ArgumentCaptor<ChangeQueuePolicyCommand> captor =
            ArgumentCaptor.forClass(ChangeQueuePolicyCommand.class);
        verify(promptQueueApplicationService).changeExecutionPolicy(org.mockito.Mockito.eq(queueId), captor.capture());
        Assertions.assertThat(captor.getValue().executionPolicy().autoRunMode()).isEqualTo(AutoRunMode.NOTIFY_ONLY);
        Assertions.assertThat(captor.getValue().executionPolicy().maxPromptsPerRun()).isEqualTo(1);
        Assertions.assertThat(captor.getValue().executionPolicy().cooldown()).isZero();
        Assertions.assertThat(captor.getValue().executionPolicy().stopOnError()).isFalse();
    }
}
