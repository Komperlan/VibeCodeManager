CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE projects (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0),
    root_directory text NOT NULL CHECK (length(btrim(root_directory)) > 0),
    status varchar(32) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT projects_root_directory_unique UNIQUE (root_directory),
    CONSTRAINT projects_updated_at_not_before_created_at CHECK (updated_at >= created_at)
);

CREATE TABLE ai_tools (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0),
    type varchar(32) NOT NULL CHECK (type IN ('FAKE', 'CODEX', 'CLAUDE_CODE', 'CUSTOM')),
    status varchar(32) NOT NULL CHECK (status IN ('ENABLED', 'DISABLED')),
    executable_path text NOT NULL CHECK (length(btrim(executable_path)) > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_tools_updated_at_not_before_created_at CHECK (updated_at >= created_at)
);

CREATE TABLE prompt_queues (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0),
    status varchar(32) NOT NULL CHECK (
        status IN (
            'CREATED',
            'WAITING_LIMIT',
            'WAITING_CONFIRMATION',
            'RUNNING',
            'PAUSED',
            'STOPPED',
            'COMPLETED',
            'DISABLED'
        )
    ),
    auto_run_mode varchar(32) NOT NULL CHECK (
        auto_run_mode IN ('NOTIFY_ONLY', 'ASK_CONFIRMATION', 'AUTO_RUN')
    ),
    max_prompts_per_run integer NOT NULL CHECK (max_prompts_per_run > 0),
    cooldown_millis bigint NOT NULL CHECK (cooldown_millis >= 0),
    stop_on_error boolean NOT NULL,
    working_hours_enabled boolean NOT NULL,
    working_hours_from time,
    working_hours_to time,
    working_hours_zone varchar(128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT prompt_queues_updated_at_not_before_created_at CHECK (updated_at >= created_at),
    CONSTRAINT prompt_queues_working_hours_complete CHECK (
        working_hours_enabled = false
        OR (
            working_hours_from IS NOT NULL
            AND working_hours_to IS NOT NULL
            AND working_hours_zone IS NOT NULL
            AND length(btrim(working_hours_zone)) > 0
        )
    )
);

CREATE TABLE prompts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_id uuid NOT NULL REFERENCES prompt_queues(id) ON DELETE CASCADE,
    target_ai_tool_id uuid NOT NULL REFERENCES ai_tools(id) ON DELETE RESTRICT,
    title varchar(150) NOT NULL CHECK (length(btrim(title)) > 0),
    content text NOT NULL CHECK (length(btrim(content)) > 0 AND length(content) <= 50000),
    status varchar(32) NOT NULL CHECK (
        status IN (
            'DRAFT',
            'QUEUED',
            'WAITING_LIMIT',
            'WAITING_CONFIRMATION',
            'RUNNING',
            'COMPLETED',
            'FAILED',
            'CANCELLED',
            'SKIPPED'
        )
    ),
    priority integer NOT NULL CHECK (priority >= 0),
    position bigint NOT NULL CHECK (position >= 0),
    working_directory_override text CHECK (
        working_directory_override IS NULL OR length(btrim(working_directory_override)) > 0
    ),
    attempt_count integer NOT NULL CHECK (attempt_count >= 0),
    max_attempts integer NOT NULL CHECK (max_attempts > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz,
    failure_reason text CHECK (
        failure_reason IS NULL OR length(btrim(failure_reason)) > 0
    ),
    CONSTRAINT prompts_attempt_count_not_above_max CHECK (attempt_count <= max_attempts),
    CONSTRAINT prompts_updated_at_not_before_created_at CHECK (updated_at >= created_at),
    CONSTRAINT prompts_finished_at_not_before_started_at CHECK (
        started_at IS NULL OR finished_at IS NULL OR finished_at >= started_at
    )
);

CREATE TABLE prompt_executions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_id uuid NOT NULL REFERENCES prompts(id) ON DELETE CASCADE,
    ai_tool_id uuid NOT NULL REFERENCES ai_tools(id) ON DELETE RESTRICT,
    status varchar(32) NOT NULL CHECK (
        status IN ('CREATED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'TIMEOUT')
    ),
    command text NOT NULL CHECK (length(btrim(command)) > 0),
    result_exit_code integer,
    result_stdout text,
    result_stderr text,
    result_raw_output text,
    result_error_message text CHECK (
        result_error_message IS NULL OR length(btrim(result_error_message)) > 0
    ),
    started_at timestamptz,
    finished_at timestamptz,
    duration_millis bigint CHECK (duration_millis IS NULL OR duration_millis >= 0),
    CONSTRAINT prompt_executions_finished_at_not_before_started_at CHECK (
        started_at IS NULL OR finished_at IS NULL OR finished_at >= started_at
    )
);

CREATE INDEX idx_prompt_queues_project_id ON prompt_queues(project_id);
CREATE INDEX idx_prompts_queue_id ON prompts(queue_id);
CREATE INDEX idx_prompts_queue_id_status ON prompts(queue_id, status);
CREATE INDEX idx_prompts_target_ai_tool_id ON prompts(target_ai_tool_id);
CREATE INDEX idx_prompt_executions_prompt_id ON prompt_executions(prompt_id);
CREATE INDEX idx_prompt_executions_ai_tool_id ON prompt_executions(ai_tool_id);
