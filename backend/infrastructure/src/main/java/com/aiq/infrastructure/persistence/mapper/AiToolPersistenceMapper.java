package com.aiq.infrastructure.persistence.mapper;

import com.aiq.domain.aitool.AiTool;
import com.aiq.infrastructure.persistence.entity.AiToolJpaEntity;

public final class AiToolPersistenceMapper {

    private AiToolPersistenceMapper() {
    }

    public static AiToolJpaEntity toEntity(AiTool aiTool) {
        return new AiToolJpaEntity(
            aiTool.getId(),
            aiTool.getName(),
            aiTool.getType(),
            aiTool.getStatus(),
            aiTool.getExecutablePath(),
            aiTool.getCreatedAt(),
            aiTool.getUpdatedAt()
        );
    }

    public static AiTool toDomain(AiToolJpaEntity entity) {
        return AiTool.restore(
            entity.getId(),
            entity.getName(),
            entity.getStatus(),
            entity.getType(),
            entity.getExecutablePath(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
