ALTER TABLE projects
    ADD COLUMN codex_session_id varchar(200);

ALTER TABLE projects
    ADD CONSTRAINT projects_codex_session_id_not_blank CHECK (
        codex_session_id IS NULL OR length(btrim(codex_session_id)) > 0
    );

ALTER TABLE prompt_executions
    ADD COLUMN external_session_id varchar(200);

ALTER TABLE prompt_executions
    ADD CONSTRAINT prompt_executions_external_session_id_not_blank CHECK (
        external_session_id IS NULL OR length(btrim(external_session_id)) > 0
    );
