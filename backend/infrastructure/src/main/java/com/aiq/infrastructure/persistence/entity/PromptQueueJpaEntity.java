package com.aiq.infrastructure.persistence.entity;

import com.aiq.domain.queue.AutoRunMode;
import com.aiq.domain.queue.QueueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "prompt_queues")
public class PromptQueueJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private QueueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_run_mode", nullable = false, length = 32)
    private AutoRunMode autoRunMode;

    @Column(name = "max_prompts_per_run", nullable = false)
    private int maxPromptsPerRun;

    @Column(name = "cooldown_millis", nullable = false)
    private long cooldownMillis;

    @Column(name = "stop_on_error", nullable = false)
    private boolean stopOnError;

    @Column(name = "working_hours_enabled", nullable = false)
    private boolean workingHoursEnabled;

    @Column(name = "working_hours_from")
    private LocalTime workingHoursFrom;

    @Column(name = "working_hours_to")
    private LocalTime workingHoursTo;

    @Column(name = "working_hours_zone", length = 128)
    private String workingHoursZone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
