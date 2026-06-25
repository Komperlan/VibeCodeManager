package com.aiq.application.project.dto;

import com.aiq.domain.project.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectDetails(
    UUID id,
    String name,
    String rootDirectory,
    String codexSessionId,
    ProjectStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
