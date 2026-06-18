package com.aiq.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aiq.application.aitool.CreateAiToolCommand;
import com.aiq.application.aitool.dto.AiToolDetails;
import com.aiq.application.aitool.dto.AiToolSummary;
import com.aiq.application.aitool.dto.CreateAiToolResult;
import com.aiq.application.port.out.AiToolRepository;
import com.aiq.domain.aitool.AiTool;
import com.aiq.domain.aitool.AiToolStatus;
import com.aiq.domain.aitool.AiToolType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiToolApplicationServiceTest {

    private final AiToolRepository aiToolRepository = mock(AiToolRepository.class);
    private final AiToolApplicationService service = new AiToolApplicationService(aiToolRepository);

    @Test
    void shouldCreateAiTool() {
        CreateAiToolCommand command = new CreateAiToolCommand(
            "  Codex  ",
            AiToolType.CODEX,
            "  /usr/local/bin/codex  "
        );
        when(aiToolRepository.save(any(AiTool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAiToolResult result = service.createAiTool(command);

        assertThat(result.aiToolId()).isNotNull();

        AiTool savedTool = savedTool();
        assertThat(savedTool.getId()).isEqualTo(result.aiToolId());
        assertThat(savedTool.getName()).isEqualTo("Codex");
        assertThat(savedTool.getType()).isEqualTo(AiToolType.CODEX);
        assertThat(savedTool.getExecutablePath()).isEqualTo("/usr/local/bin/codex");
        assertThat(savedTool.getStatus()).isEqualTo(AiToolStatus.ENABLED);
    }

    @Test
    void shouldRejectNullCreateCommand() {
        assertThatThrownBy(() -> service.createAiTool(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Create AI tool command must not be null");

        verifyNoInteractions(aiToolRepository);
    }

    @Test
    void shouldReturnAiToolDetails() {
        AiTool tool = aiTool("Codex", AiToolStatus.ENABLED);
        givenTool(tool);

        AiToolDetails details = service.getAiTool(tool.getId());

        assertThat(details.id()).isEqualTo(tool.getId());
        assertThat(details.name()).isEqualTo("Codex");
        assertThat(details.type()).isEqualTo(AiToolType.CODEX);
        assertThat(details.status()).isEqualTo(AiToolStatus.ENABLED);
        assertThat(details.executablePath()).isEqualTo("/usr/local/bin/codex");
        assertThat(details.createdAt()).isEqualTo(tool.getCreatedAt());
        assertThat(details.updatedAt()).isEqualTo(tool.getUpdatedAt());
    }

    @Test
    void shouldListAiToolSummaries() {
        AiTool codex = aiTool("Codex", AiToolStatus.ENABLED);
        AiTool claude = AiTool.restore(
            UUID.randomUUID(),
            "Claude Code",
            AiToolStatus.DISABLED,
            AiToolType.CLAUDE_CODE,
            "/usr/local/bin/claude",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
        when(aiToolRepository.findAll()).thenReturn(List.of(codex, claude));

        List<AiToolSummary> tools = service.listAiTools();

        assertThat(tools)
            .extracting(AiToolSummary::id)
            .containsExactly(codex.getId(), claude.getId());
        assertThat(tools)
            .extracting(AiToolSummary::status)
            .containsExactly(AiToolStatus.ENABLED, AiToolStatus.DISABLED);
    }

    @Test
    void shouldRenameAiToolAndSaveIt() {
        AiTool tool = aiTool("Codex", AiToolStatus.ENABLED);
        givenTool(tool);

        service.renameAiTool(tool.getId(), "Primary Codex");

        assertThat(tool.getName()).isEqualTo("Primary Codex");
        verify(aiToolRepository).save(tool);
    }

    @Test
    void shouldChangeExecutablePathAndSaveTool() {
        AiTool tool = aiTool("Codex", AiToolStatus.ENABLED);
        givenTool(tool);

        service.changeExecutablePath(tool.getId(), "/opt/codex");

        assertThat(tool.getExecutablePath()).isEqualTo("/opt/codex");
        verify(aiToolRepository).save(tool);
    }

    @Test
    void shouldEnableAiToolAndSaveIt() {
        AiTool tool = aiTool("Codex", AiToolStatus.DISABLED);
        givenTool(tool);

        service.enableAiTool(tool.getId());

        assertThat(tool.getStatus()).isEqualTo(AiToolStatus.ENABLED);
        verify(aiToolRepository).save(tool);
    }

    @Test
    void shouldDisableAiToolAndSaveIt() {
        AiTool tool = aiTool("Codex", AiToolStatus.ENABLED);
        givenTool(tool);

        service.disableAiTool(tool.getId());

        assertThat(tool.getStatus()).isEqualTo(AiToolStatus.DISABLED);
        verify(aiToolRepository).save(tool);
    }

    @Test
    void shouldRejectMissingAiTool() {
        UUID aiToolId = UUID.randomUUID();
        when(aiToolRepository.findById(aiToolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAiTool(aiToolId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AI tool not found: " + aiToolId);

        verify(aiToolRepository).findById(aiToolId);
        verify(aiToolRepository, never()).save(any(AiTool.class));
    }

    private AiTool aiTool(String name, AiToolStatus status) {
        return AiTool.restore(
            UUID.randomUUID(),
            name,
            status,
            AiToolType.CODEX,
            "/usr/local/bin/codex",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }

    private void givenTool(AiTool tool) {
        when(aiToolRepository.findById(tool.getId())).thenReturn(Optional.of(tool));
    }

    private AiTool savedTool() {
        ArgumentCaptor<AiTool> captor = ArgumentCaptor.forClass(AiTool.class);
        verify(aiToolRepository).save(captor.capture());
        return captor.getValue();
    }
}
