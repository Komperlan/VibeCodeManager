package com.aiq.application.port.out;

import com.aiq.domain.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findById(UUID projectId);

    List<Project> findAll();

    boolean existsByRootDirectory(String rootDirectory);
}
