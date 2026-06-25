package com.aiq.infrastructure.persistence.mapper;

import com.aiq.domain.project.Project;
import com.aiq.infrastructure.persistence.entity.ProjectJpaEntity;

public final class ProjectPersistenceMapper {

    private ProjectPersistenceMapper() {
    }

    public static ProjectJpaEntity toEntity(Project project) {
        return new ProjectJpaEntity(
            project.getId(),
            project.getName(),
            project.getRootDirectory(),
            project.getCodexSessionId(),
            project.getStatus(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }

    public static Project toDomain(ProjectJpaEntity entity) {
        return Project.restore(
            entity.getId(),
            entity.getName(),
            entity.getRootDirectory(),
            entity.getCodexSessionId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
