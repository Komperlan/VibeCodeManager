package com.aiq.application.aitool.dto;

import java.time.Instant;
import java.util.UUID;

import com.aiq.domain.aitool.AiToolStatus;
import com.aiq.domain.aitool.AiToolType;

public record AiToolDetails (
    UUID id,
    String name,
    AiToolStatus status,
    AiToolType type,
    String executablePath,
    Instant createdAt,
    Instant updatedAt
) {}