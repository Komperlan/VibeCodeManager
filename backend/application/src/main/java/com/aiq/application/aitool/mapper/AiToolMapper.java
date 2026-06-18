package com.aiq.application.aitool.mapper;

import com.aiq.application.aitool.dto.AiToolDetails;
import com.aiq.application.aitool.dto.AiToolSummary;
import com.aiq.domain.aitool.AiTool;

public final class AiToolMapper {

    private AiToolMapper() {
    }

    public static AiToolSummary toSummary(AiTool aiTool) {
        return new AiToolSummary(
            aiTool.getId(),
            aiTool.getName(),
            aiTool.getType(),
            aiTool.getStatus()
        );
    }

    public static AiToolDetails toDetails(AiTool aiTool) {
        return new AiToolDetails(
            aiTool.getId(),
            aiTool.getName(),
            aiTool.getStatus(),
            aiTool.getType(),
            aiTool.getExecutablePath(),
            aiTool.getCreatedAt(),
            aiTool.getUpdatedAt()
        );
    }
}
