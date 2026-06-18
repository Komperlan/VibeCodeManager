package com.aiq.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aiq.domain.aitool.AiTool;

public interface AiToolRepository {

    AiTool save(AiTool tool);

    Optional<AiTool> findById(UUID id);
    
    List<AiTool> findAll();
}
