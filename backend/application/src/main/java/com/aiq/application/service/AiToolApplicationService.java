package com.aiq.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiq.application.aitool.CreateAiToolCommand;
import com.aiq.application.aitool.dto.AiToolDetails;
import com.aiq.application.aitool.dto.AiToolSummary;
import com.aiq.application.aitool.dto.CreateAiToolResult;
import com.aiq.application.aitool.mapper.AiToolMapper;
import com.aiq.application.port.out.AiToolRepository;
import com.aiq.domain.aitool.AiTool;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AiToolApplicationService {
    private final AiToolRepository aiToolRepository;

    public CreateAiToolResult createAiTool(CreateAiToolCommand command) {
        Objects.requireNonNull(command, "Create AI tool command must not be null");
        AiTool tool = AiTool.create(command.name(), command.type(), command.executablePath());
        AiTool savedTool = aiToolRepository.save(tool);
        return new CreateAiToolResult(savedTool.getId());
    }

    @Transactional(readOnly = true)
    public AiToolDetails getAiTool(UUID aiToolId) {
        return AiToolMapper.toDetails(findAiToolRequired(aiToolId));
    }

    @Transactional(readOnly = true)
    public List<AiToolSummary> listAiTools() {
        return aiToolRepository.findAll().stream()
            .map(AiToolMapper::toSummary)
            .toList();
    }

    public void renameAiTool(UUID aiToolId, String name) {
        AiTool tool = findAiToolRequired(aiToolId);
        tool.rename(name);
        aiToolRepository.save(tool);
    }

    public void changeExecutablePath(UUID aiToolId, String executablePath) {
        AiTool tool = findAiToolRequired(aiToolId);
        tool.changeExecutablePath(executablePath);
        aiToolRepository.save(tool);
    }

    public void enableAiTool(UUID aiToolId) {
        AiTool tool = findAiToolRequired(aiToolId);
        tool.enable();
        aiToolRepository.save(tool);
    }

    public void disableAiTool(UUID aiToolId) {
        AiTool tool = findAiToolRequired(aiToolId);
        tool.disable();
        aiToolRepository.save(tool);
    }

    private AiTool findAiToolRequired(UUID toolId) {
        Objects.requireNonNull(toolId, "Tool id must not be null");
        return aiToolRepository.findById(toolId)
            .orElseThrow(() -> new IllegalArgumentException("AI tool not found: " + toolId));
    }
}
