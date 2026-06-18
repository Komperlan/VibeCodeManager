package com.aiq.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.aiq.application.port.out.AiToolRepository;
import com.aiq.domain.aitool.AiTool;
import com.aiq.infrastructure.persistence.mapper.AiToolPersistenceMapper;
import com.aiq.infrastructure.persistence.repositories.AiToolJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AiToolRepositoryAdapter implements AiToolRepository {
    private final AiToolJpaRepository aiToolJpaRepository;

    @Override
    public AiTool save(AiTool tool) {
        var entity = AiToolPersistenceMapper.toEntity(tool);
        var savedEntity = aiToolJpaRepository.save(entity);
        return AiToolPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AiTool> findById(UUID id) {
        return aiToolJpaRepository.findById(id).map(AiToolPersistenceMapper::toDomain);
    }

    @Override
    public List<AiTool> findAll() {
        return aiToolJpaRepository.findAll().stream().map(AiToolPersistenceMapper::toDomain).toList();
    }
    
}
