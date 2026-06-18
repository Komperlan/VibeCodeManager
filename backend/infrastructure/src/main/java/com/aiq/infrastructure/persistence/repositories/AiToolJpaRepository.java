package com.aiq.infrastructure.persistence.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiq.infrastructure.persistence.entity.AiToolJpaEntity;

public interface AiToolJpaRepository extends JpaRepository<AiToolJpaEntity, UUID> {
}
