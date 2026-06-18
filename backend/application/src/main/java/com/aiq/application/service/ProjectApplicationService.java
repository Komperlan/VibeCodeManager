package com.aiq.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiq.application.port.out.ProjectRepository;
import com.aiq.application.project.CreateProjectCommand;
import com.aiq.application.project.dto.CreateProjectResult;
import com.aiq.application.project.dto.ProjectDetails;
import com.aiq.application.project.dto.ProjectSummary;
import com.aiq.application.project.mapper.ProjectMapper;
import com.aiq.domain.project.Project;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;

    public CreateProjectResult createProject(CreateProjectCommand command) {
        Objects.requireNonNull(command, "Create project command must not be null");
        if (projectRepository.existsByRootDirectory(command.rootDirectory())) {
            throw new IllegalArgumentException("Project with this root directory already exists");
        }

        Project project = Project.create(command.name(), command.rootDirectory());
        Project savedProject = projectRepository.save(project);
        return ProjectMapper.toCreateProjectResult(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectDetails getProject(UUID projectId) {
        return ProjectMapper.toDetails(findProjectRequired(projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listProjects() {
        return projectRepository.findAll().stream()
            .map(ProjectMapper::toSummary)
            .toList();
    }

    public void renameProject(UUID projectId, String name) {
        Project project = findProjectRequired(projectId);
        project.rename(name);
        projectRepository.save(project);
    }

    public void changeRootDirectory(UUID projectId, String rootDirectory) {
        Project project = findProjectRequired(projectId);
        project.changeRootDirectory(rootDirectory);
        projectRepository.save(project);
    }

    public void disableProject(UUID projectId) {
        Project project = findProjectRequired(projectId);
        project.disable();
        projectRepository.save(project);
    }

    public void activateProject(UUID projectId) {
        Project project = findProjectRequired(projectId);
        project.activate();
        projectRepository.save(project);
    }

    public void archiveProject(UUID projectId) {
        Project project = findProjectRequired(projectId);
        project.archive();
        projectRepository.save(project);
    }

    private Project findProjectRequired(UUID projectId) {
        Objects.requireNonNull(projectId, "Project id must not be null");
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }
}
