package com.aiq.infrastructure.persistence.adapter;

import com.aiq.application.port.out.ProjectRepository;
import com.aiq.domain.project.Project;
import com.aiq.infrastructure.persistence.mapper.ProjectPersistenceMapper;
import com.aiq.infrastructure.persistence.repositories.ProjectJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        var entity = ProjectPersistenceMapper.toEntity(project);
        var savedEntity = projectJpaRepository.save(entity);
        return ProjectPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Project> findById(UUID projectId) {
        return projectJpaRepository.findById(projectId)
            .map(ProjectPersistenceMapper::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return projectJpaRepository.findAll().stream()
            .map(ProjectPersistenceMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByRootDirectory(String rootDirectory) {
        return projectJpaRepository.existsByRootDirectory(rootDirectory);
    }
}
