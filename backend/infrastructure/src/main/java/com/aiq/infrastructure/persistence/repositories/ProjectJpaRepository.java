package com.aiq.infrastructure.persistence.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiq.infrastructure.persistence.entity.ProjectJpaEntity;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {
    boolean existsByRootDirectory(String rootDirectory);
}
