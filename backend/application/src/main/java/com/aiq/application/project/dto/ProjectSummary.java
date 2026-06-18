package com.aiq.application.project.dto;

import com.aiq.domain.project.ProjectStatus;
import java.util.UUID;

public record ProjectSummary(
    UUID id,
    String name,
    ProjectStatus status
) {
}
