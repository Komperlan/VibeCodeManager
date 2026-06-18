package com.aiq.adapters.web.aitool.controller;

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
import com.aiq.application.aitool.CreateAiToolCommand;
import com.aiq.application.aitool.dto.AiToolSummary;
import com.aiq.application.aitool.dto.CreateAiToolResult;
import com.aiq.application.service.AiToolApplicationService;
import com.aiq.domain.aitool.AiToolStatus;
import com.aiq.domain.aitool.AiToolType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

class AiToolControllerTest extends ControllerTestSupport {

    private final AiToolApplicationService aiToolApplicationService = mock(AiToolApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new AiToolController(aiToolApplicationService));
    }

    @Test
    void shouldCreateAiTool() throws Exception {
        UUID aiToolId = UUID.randomUUID();
        when(aiToolApplicationService.createAiTool(any(CreateAiToolCommand.class)))
            .thenReturn(new CreateAiToolResult(aiToolId));

        mockMvc.perform(post("/api/v1/ai-tools")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "name", "Codex",
                    "type", "CODEX",
                    "executablePath", "/usr/bin/codex"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.aiToolId").value(aiToolId.toString()));

        ArgumentCaptor<CreateAiToolCommand> captor = ArgumentCaptor.forClass(CreateAiToolCommand.class);
        verify(aiToolApplicationService).createAiTool(captor.capture());
        CreateAiToolCommand command = captor.getValue();
        Assertions.assertThat(command.name()).isEqualTo("Codex");
        Assertions.assertThat(command.type()).isEqualTo(AiToolType.CODEX);
        Assertions.assertThat(command.executablePath()).isEqualTo("/usr/bin/codex");
    }

    @Test
    void shouldRejectInvalidAiToolType() throws Exception {
        mockMvc.perform(post("/api/v1/ai-tools")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "name": "Unknown",
                      "type": "UNKNOWN",
                      "executablePath": "/usr/bin/unknown"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));

        verifyNoInteractions(aiToolApplicationService);
    }

    @Test
    void shouldListAiTools() throws Exception {
        UUID aiToolId = UUID.randomUUID();
        when(aiToolApplicationService.listAiTools()).thenReturn(List.of(
            new AiToolSummary(aiToolId, "Codex", AiToolType.CODEX, AiToolStatus.ENABLED)
        ));

        mockMvc.perform(get("/api/v1/ai-tools"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(aiToolId.toString()))
            .andExpect(jsonPath("$[0].name").value("Codex"))
            .andExpect(jsonPath("$[0].type").value("CODEX"))
            .andExpect(jsonPath("$[0].status").value("ENABLED"));
    }

    @Test
    void shouldRenameAiTool() throws Exception {
        UUID aiToolId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/ai-tools/{aiToolId}/name", aiToolId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("name", "Claude Code"))))
            .andExpect(status().isNoContent());

        verify(aiToolApplicationService).renameAiTool(aiToolId, "Claude Code");
    }

    @Test
    void shouldRejectBlankExecutablePath() throws Exception {
        UUID aiToolId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/ai-tools/{aiToolId}/executable-path", aiToolId)
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("executablePath", "   "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.executablePath[0]").value("executable path must not be blank"));
    }
}
