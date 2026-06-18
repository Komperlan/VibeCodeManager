package com.aiq.infrastructure.persistence.entity;

import com.aiq.domain.execution.ExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "prompt_executions")
public class PromptExecutionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(name = "ai_tool_id", nullable = false)
    private UUID aiToolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExecutionStatus status;

    @Column(name = "command", nullable = false, columnDefinition = "text")
    private String command;

    @Column(name = "result_exit_code")
    private Integer resultExitCode;

    @Column(name = "result_stdout", columnDefinition = "text")
    private String resultStdout;

    @Column(name = "result_stderr", columnDefinition = "text")
    private String resultStderr;

    @Column(name = "result_raw_output", columnDefinition = "text")
    private String resultRawOutput;

    @Column(name = "result_error_message", columnDefinition = "text")
    private String resultErrorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_millis")
    private Long durationMillis;
}
