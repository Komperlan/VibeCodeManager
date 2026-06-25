package com.aiq.application.project.mapper;

import com.aiq.application.project.dto.CreateProjectResult;
import com.aiq.application.project.dto.ProjectDetails;
import com.aiq.application.project.dto.ProjectSummary;
import com.aiq.domain.project.Project;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static CreateProjectResult toCreateProjectResult(Project project) {
        return new CreateProjectResult(project.getId());
    }

    public static ProjectDetails toDetails(Project project) {
        return new ProjectDetails(
            project.getId(),
            project.getName(),
            project.getRootDirectory(),
            project.getCodexSessionId(),
            project.getStatus(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }

    public static ProjectSummary toSummary(Project project) {
        return new ProjectSummary(
            project.getId(),
            project.getName(),
            project.getCodexSessionId(),
            project.getStatus()
        );
    }
}
