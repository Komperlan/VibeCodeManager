package com.aiq.domain.aitool;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.aiq.domain.common.AggregateRoot;

import lombok.Getter;

@Getter
public class AiTool extends AggregateRoot {
    private static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private String name;
    private AiToolStatus status;
    private final AiToolType type;
    private String executablePath;
    private final Instant createdAt;
    private Instant updatedAt;

    private AiTool(
        UUID id,
        String name,
        AiToolStatus status,
        AiToolType type,
        String executablePath,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "AiTool id must not be null");
        this.name = validateName(name);
        this.status = Objects.requireNonNull(status, "AiTool status must not be null");
        this.type = Objects.requireNonNull(type, "AiTool type must not be null");
        this.executablePath = validateExecutablePath(executablePath);
        this.createdAt = Objects.requireNonNull(createdAt, "AiTool createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "AiTool updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("AiTool updatedAt must not be before createdAt");
        }
    }

    public static AiTool create(String name, AiToolType type, String executablePath) {
        Instant now = Instant.now();
        return new AiTool(UUID.randomUUID(), name, AiToolStatus.ENABLED, type, executablePath, now, now);
    }

    public static AiTool restore(
        UUID id,
        String name,
        AiToolStatus status,
        AiToolType type,
        String executablePath,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new AiTool(id, name, status, type, executablePath, createdAt, updatedAt);
    }

    public void rename(String newName) {
        String normalizedName = validateName(newName);
        this.name = normalizedName;
        this.updatedAt = Instant.now();
    }

    public void changeExecutablePath(String executablePath) {
        String normalizedExecutablePath = validateExecutablePath(executablePath);
        this.executablePath = normalizedExecutablePath;
        this.updatedAt = Instant.now();
    }

    public void enable() {
        this.updatedAt = Instant.now();
        this.status = AiToolStatus.ENABLED;
    }

    public void disable() {
        this.updatedAt = Instant.now();
        this.status = AiToolStatus.DISABLED;
    }

    public boolean isEnabled() {
        return this.status == AiToolStatus.ENABLED;
    }

    private static String validateName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AiTool name must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("AiTool name must not be blank");
        }
        if (normalizedValue.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("AiTool name must be at most 100 characters");
        }

        return normalizedValue;
    }

    private static String validateExecutablePath(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AiTool executable path must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("AiTool executable path must not be blank");
        }

        return normalizedValue;
    }
}
