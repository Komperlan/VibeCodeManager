package com.aiq.domain.project;

import com.aiq.domain.common.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Project extends AggregateRoot {

    private static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private String name;
    private String rootDirectory;
    private String codexSessionId;
    private ProjectStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Project(
        UUID id,
        String name,
        String rootDirectory,
        String codexSessionId,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Project id must not be null");
        this.name = validateName(name);
        this.rootDirectory = validateRootDirectory(rootDirectory);
        this.codexSessionId = normalizeCodexSessionId(codexSessionId);
        this.status = Objects.requireNonNull(status, "Project status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Project updatedAt must not be before createdAt");
        }
    }

    public static Project create(String name, String rootDirectory) {
        return create(name, rootDirectory, null);
    }

    public static Project create(String name, String rootDirectory, String codexSessionId) {
        Instant now = Instant.now();

        return new Project(
            UUID.randomUUID(),
            name,
            rootDirectory,
            codexSessionId,
            ProjectStatus.ACTIVE,
            now,
            now
        );
    }

    public static Project restore(
        UUID id,
        String name,
        String rootDirectory,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        return restore(id, name, rootDirectory, null, status, createdAt, updatedAt);
    }

    public static Project restore(
        UUID id,
        String name,
        String rootDirectory,
        String codexSessionId,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Project(
            id,
            name,
            rootDirectory,
            codexSessionId,
            status,
            createdAt,
            updatedAt
        );
    }

    public void rename(String newName) {
        ensureNotArchived();
        this.name = validateName(newName);
        this.updatedAt = Instant.now();
    }

    public void changeRootDirectory(String newRootDirectory) {
        ensureNotArchived();
        this.rootDirectory = validateRootDirectory(newRootDirectory);
        this.updatedAt = Instant.now();
    }

    public void attachCodexSession(String newCodexSessionId) {
        ensureNotArchived();
        this.codexSessionId = validateCodexSessionId(newCodexSessionId);
        this.updatedAt = Instant.now();
    }

    public void clearCodexSession() {
        ensureNotArchived();
        this.codexSessionId = null;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        ensureNotArchived();
        this.status = ProjectStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        ensureNotArchived();
        this.status = ProjectStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }

    public boolean hasCodexSession() {
        return codexSessionId != null;
    }

    private void ensureNotArchived() {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Archived project cannot be changed");
        }
    }

    private static String validateName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Project name must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        if (normalizedValue.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Project name must be at most 100 characters");
        }

        return normalizedValue;
    }

    private static String validateRootDirectory(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Project root directory must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Project root directory must not be blank");
        }

        return normalizedValue;
    }

    private static String normalizeCodexSessionId(String value) {
        if (value == null) {
            return null;
        }

        return validateCodexSessionId(value);
    }

    private static String validateCodexSessionId(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Project Codex session id must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Project Codex session id must not be blank");
        }
        if (normalizedValue.length() > 200) {
            throw new IllegalArgumentException("Project Codex session id must be at most 200 characters");
        }

        return normalizedValue;
    }
}
