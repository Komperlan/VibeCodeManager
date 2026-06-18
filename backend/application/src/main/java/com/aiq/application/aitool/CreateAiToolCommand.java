package com.aiq.application.aitool;

import com.aiq.domain.aitool.AiToolType;

public record CreateAiToolCommand(
    String name,
    AiToolType type,
    String executablePath
) {
}
