package com.aiq.application.aitool.dto;

import java.util.UUID;

import com.aiq.domain.aitool.AiToolStatus;
import com.aiq.domain.aitool.AiToolType;

public record AiToolSummary (
    UUID id,
    String name,
    AiToolType type,
    AiToolStatus status
) {}