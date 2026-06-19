import { type FormEvent, useEffect, useState } from 'react';
import { Button, EmptyState, Field, inputClassName, PageHeader, Panel, PanelHeader, ProgressBar, StatusBadge } from '../components/ui';
import { cn } from '../lib/cn';
import type { AiTool, AutoRunMode, CreatePromptInput, CreateQueueInput, Project, Prompt, PromptDetails, Queue } from '../types';

interface QueuesPageProps {
  projects: Project[];
  tools: AiTool[];
  queues: Queue[];
  prompts: Prompt[];
  selectedProjectId: string | null;
  selectedQueueId: string | null;
  selectedPromptDetails: PromptDetails | null;
  onSelectProject: (projectId: string) => void;
  onSelectQueue: (queueId: string) => void;
  onSelectPrompt: (promptId: string) => void;
  loading: boolean;
  onCreateQueue: (input: CreateQueueInput) => void;
  onCreatePrompt: (input: CreatePromptInput) => void;
  onRunQueue: (queueId: string) => void;
}

export function QueuesPage(props: QueuesPageProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showPromptForm, setShowPromptForm] = useState(false);
  const [form, setForm] = useState<CreateQueueInput>({
    projectId: props.selectedProjectId ?? props.projects[0]?.id ?? '',
    name: '',
    autoRunMode: 'ASK_CONFIRMATION',
    maxPromptsPerRun: 3,
    cooldownSeconds: 60,
    stopOnError: true,
  });
  const visibleQueues = props.selectedProjectId
    ? props.queues.filter((queue) => queue.projectId === props.selectedProjectId)
    : props.queues;
  const availableTools = props.tools.filter((tool) => tool.status === 'ENABLED');
  const promptQueueOptions = visibleQueues.length > 0 ? visibleQueues : props.queues;
  const [promptForm, setPromptForm] = useState<CreatePromptInput>({
    queueId: props.selectedQueueId ?? props.queues[0]?.id ?? '',
    targetAiToolId: availableTools[0]?.id ?? '',
    title: '',
    content: '',
    priority: 0,
    maxAttempts: 3,
    workingDirectoryOverride: null,
  });

  useEffect(() => {
    if (form.projectId || props.projects.length === 0) {
      return;
    }

    setForm((current) => ({
      ...current,
      projectId: props.selectedProjectId ?? props.projects[0].id,
    }));
  }, [form.projectId, props.projects, props.selectedProjectId]);

  useEffect(() => {
    const preferredQueueId = props.selectedQueueId && promptQueueOptions.some((queue) => queue.id === props.selectedQueueId)
      ? props.selectedQueueId
      : promptQueueOptions[0]?.id ?? '';
    const nextQueueId = promptQueueOptions.some((queue) => queue.id === promptForm.queueId)
      ? promptForm.queueId
      : preferredQueueId;
    const nextToolId = availableTools.some((tool) => tool.id === promptForm.targetAiToolId)
      ? promptForm.targetAiToolId
      : availableTools[0]?.id ?? '';

    if (nextQueueId === promptForm.queueId && nextToolId === promptForm.targetAiToolId) {
      return;
    }

    setPromptForm((current) => ({
      ...current,
      queueId: nextQueueId,
      targetAiToolId: nextToolId,
    }));
  }, [availableTools, promptForm.queueId, promptForm.targetAiToolId, promptQueueOptions, props.selectedQueueId]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const projectId = form.projectId || props.selectedProjectId || props.projects[0]?.id;
    if (!projectId) {
      return;
    }

    props.onCreateQueue({
      ...form,
      projectId,
      name: form.name.trim(),
      maxPromptsPerRun: Math.max(1, form.maxPromptsPerRun),
      cooldownSeconds: Math.max(0, form.cooldownSeconds),
    });
    setForm((current) => ({ ...current, projectId, name: '' }));
    setShowCreateForm(false);
  }

  function handlePromptSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const queueId = promptForm.queueId || promptQueueOptions[0]?.id;
    const targetAiToolId = promptForm.targetAiToolId || availableTools[0]?.id;
    if (!queueId || !targetAiToolId) {
      return;
    }

    props.onCreatePrompt({
      ...promptForm,
      queueId,
      targetAiToolId,
      title: promptForm.title.trim(),
      content: promptForm.content.trim(),
      priority: Math.max(0, promptForm.priority),
      maxAttempts: Math.max(1, promptForm.maxAttempts),
      workingDirectoryOverride: promptForm.workingDirectoryOverride?.trim() || null,
    });
    setPromptForm((current) => ({
      ...current,
      queueId,
      targetAiToolId,
      title: '',
      content: '',
      workingDirectoryOverride: null,
    }));
    setShowPromptForm(false);
  }

  function togglePromptForm() {
    setPromptForm((current) => ({
      ...current,
      queueId: props.selectedQueueId ?? promptQueueOptions[0]?.id ?? current.queueId,
      targetAiToolId: current.targetAiToolId || (availableTools[0]?.id ?? ''),
    }));
    setShowPromptForm((visible) => !visible);
  }

  return (
    <>
      <PageHeader
        eyebrow="Prompt Pipelines"
        title="Queues"
        description="Queues hold ordered prompts, run policies, limit-aware status, and execution progress. Prompt positions are append-only, so history stays stable."
        actions={
          <>
            <Button
              disabled={props.loading || props.queues.length === 0 || availableTools.length === 0}
              onClick={togglePromptForm}
              variant="secondary"
            >
              {showPromptForm ? 'Close Prompt' : 'Add Prompt'}
            </Button>
            <Button disabled={props.loading || props.projects.length === 0} onClick={() => setShowCreateForm((visible) => !visible)} variant="primary">
              {showCreateForm ? 'Close Queue' : 'Create Queue'}
            </Button>
          </>
        }
      />

      {showCreateForm && (
        <Panel className="mb-5">
          <PanelHeader eyebrow="Create" title="New queue" description="Create a prompt pipeline with a simple execution policy." />
          <form className="grid gap-4 xl:grid-cols-[1fr_1fr_0.9fr_0.7fr_0.7fr_auto]" onSubmit={handleSubmit}>
            <Field label="Project">
              <select
                className={inputClassName}
                onChange={(event) => setForm((current) => ({ ...current, projectId: event.target.value }))}
                required
                value={form.projectId}
              >
                {props.projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Queue name">
              <input
                className={inputClassName}
                maxLength={100}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Backend implementation"
                required
                value={form.name}
              />
            </Field>
            <Field label="Run mode">
              <select
                className={inputClassName}
                onChange={(event) => setForm((current) => ({ ...current, autoRunMode: event.target.value as AutoRunMode }))}
                value={form.autoRunMode}
              >
                <option value="ASK_CONFIRMATION">ASK_CONFIRMATION</option>
                <option value="NOTIFY_ONLY">NOTIFY_ONLY</option>
                <option value="AUTO_RUN">AUTO_RUN</option>
              </select>
            </Field>
            <Field label="Max prompts">
              <input
                className={inputClassName}
                min={1}
                onChange={(event) => setForm((current) => ({ ...current, maxPromptsPerRun: Number(event.target.value) }))}
                required
                type="number"
                value={form.maxPromptsPerRun}
              />
            </Field>
            <Field label="Cooldown, sec">
              <input
                className={inputClassName}
                min={0}
                onChange={(event) => setForm((current) => ({ ...current, cooldownSeconds: Number(event.target.value) }))}
                required
                type="number"
                value={form.cooldownSeconds}
              />
            </Field>
            <div className="flex flex-col justify-end gap-3">
              <label className="flex items-center gap-2 text-sm font-bold text-slate-300">
                <input
                  checked={form.stopOnError}
                  className="h-4 w-4 accent-violet-500"
                  onChange={(event) => setForm((current) => ({ ...current, stopOnError: event.target.checked }))}
                  type="checkbox"
                />
                Stop on error
              </label>
              <Button disabled={props.loading || props.projects.length === 0} type="submit" variant="primary">
                Create
              </Button>
            </div>
          </form>
        </Panel>
      )}

      {showPromptForm && (
        <Panel className="mb-5">
          <PanelHeader eyebrow="Create" title="Add prompt" description="Append a prompt to the selected queue. Position is assigned by the backend." />
          <form className="grid gap-4" onSubmit={handlePromptSubmit}>
            <div className="grid gap-4 xl:grid-cols-[1fr_1fr_1.2fr_0.55fr_0.55fr]">
              <Field label="Queue">
                <select
                  className={inputClassName}
                  onChange={(event) => setPromptForm((current) => ({ ...current, queueId: event.target.value }))}
                  required
                  value={promptForm.queueId}
                >
                  {promptQueueOptions.map((queue) => (
                    <option key={queue.id} value={queue.id}>
                      {queue.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="AI tool">
                <select
                  className={inputClassName}
                  onChange={(event) => setPromptForm((current) => ({ ...current, targetAiToolId: event.target.value }))}
                  required
                  value={promptForm.targetAiToolId}
                >
                  {availableTools.map((tool) => (
                    <option key={tool.id} value={tool.id}>
                      {tool.name} / {tool.type}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Title">
                <input
                  className={inputClassName}
                  maxLength={150}
                  onChange={(event) => setPromptForm((current) => ({ ...current, title: event.target.value }))}
                  placeholder="Fix failing Maven tests"
                  required
                  value={promptForm.title}
                />
              </Field>
              <Field label="Priority">
                <input
                  className={inputClassName}
                  min={0}
                  onChange={(event) => setPromptForm((current) => ({ ...current, priority: Number(event.target.value) }))}
                  required
                  type="number"
                  value={promptForm.priority}
                />
              </Field>
              <Field label="Attempts">
                <input
                  className={inputClassName}
                  min={1}
                  onChange={(event) => setPromptForm((current) => ({ ...current, maxAttempts: Number(event.target.value) }))}
                  required
                  type="number"
                  value={promptForm.maxAttempts}
                />
              </Field>
            </div>
            <Field label="Prompt content">
              <textarea
                className={inputClassName}
                maxLength={50000}
                onChange={(event) => setPromptForm((current) => ({ ...current, content: event.target.value }))}
                placeholder="Describe the task for the AI tool..."
                required
                rows={6}
                value={promptForm.content}
              />
            </Field>
            <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
              <Field label="Working directory override, optional">
                <input
                  className={inputClassName}
                  onChange={(event) => setPromptForm((current) => ({ ...current, workingDirectoryOverride: event.target.value }))}
                  placeholder="/home/dryu/gits/VibeCodeManager"
                  value={promptForm.workingDirectoryOverride ?? ''}
                />
              </Field>
              <div className="flex items-end">
                <Button disabled={props.loading || promptQueueOptions.length === 0 || availableTools.length === 0} type="submit" variant="primary">
                  Add Prompt
                </Button>
              </div>
            </div>
          </form>
        </Panel>
      )}

      <section className="grid gap-5 xl:grid-cols-[0.8fr_1.2fr]">
        <Panel>
          <PanelHeader eyebrow="Filter" title="Project" />
          <div className="grid gap-3">
            <button
              className={cn(
                'rounded-2xl border px-4 py-3 text-left text-sm font-bold transition',
                props.selectedProjectId === null ? 'border-violet-300/40 bg-violet-400/15 text-white' : 'border-white/10 bg-white/[0.04] text-slate-200',
              )}
              onClick={() => props.onSelectProject('')}
              type="button"
            >
              All projects
            </button>
            {props.projects.map((project) => (
              <button
                className={cn(
                  'rounded-2xl border px-4 py-3 text-left transition hover:border-violet-300/30 hover:bg-white/[0.07]',
                  props.selectedProjectId === project.id ? 'border-violet-300/40 bg-violet-400/15' : 'border-white/10 bg-white/[0.04]',
                )}
                key={project.id}
                onClick={() => props.onSelectProject(project.id)}
                type="button"
              >
                <div className="font-bold text-white">{project.name}</div>
                <div className="mt-1 text-sm text-slate-500">{project.queueCount} queues</div>
              </button>
            ))}
          </div>
        </Panel>

        <Panel>
          <PanelHeader eyebrow="Queues" title="Execution backlog" />
          <div className="grid gap-4">
            {visibleQueues.length === 0 && <EmptyState title="No queues" description="Create a queue for the selected project." />}
            {visibleQueues.map((queue) => {
              const queuePrompts = props.prompts.filter((prompt) => prompt.queueId === queue.id);
              const total = queue.queuedPrompts + queue.completedPrompts + queue.failedPrompts;
              const progress = total === 0 ? 0 : (queue.completedPrompts / total) * 100;
              const selected = props.selectedQueueId === queue.id;

              return (
                <article
                  className={cn(
                    'cursor-pointer rounded-3xl border p-5 transition hover:border-violet-300/35 hover:bg-white/[0.07]',
                    selected ? 'border-violet-300/50 bg-violet-500/[0.13] shadow-xl shadow-violet-950/20' : 'border-white/10 bg-white/[0.04]',
                  )}
                  key={queue.id}
                  onClick={() => props.onSelectQueue(queue.id)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      props.onSelectQueue(queue.id);
                    }
                  }}
                  role="button"
                  tabIndex={0}
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <div className="text-xl font-black text-white">{queue.name}</div>
                        {selected && <span className="rounded-full border border-violet-300/30 bg-violet-400/15 px-3 py-1 text-xs font-black uppercase tracking-wide text-violet-100">Selected</span>}
                      </div>
                      <div className="mt-1 text-sm text-slate-500">
                        {queue.autoRunMode} / max {queue.maxPromptsPerRun} / cooldown {queue.cooldownSeconds}s
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <StatusBadge status={queue.status} />
                      <Button disabled={props.loading} onClick={() => props.onRunQueue(queue.id)} variant="secondary">
                        {queue.status === 'WAITING_LIMIT' ? 'Retry' : 'Run'}
                      </Button>
                    </div>
                  </div>

                  <div className="mt-5">
                    <div className="mb-2 flex justify-between text-xs text-slate-500">
                      <span>{queue.completedPrompts} completed</span>
                      <span>{queue.queuedPrompts} queued</span>
                    </div>
                    <ProgressBar value={progress} />
                  </div>

                  <div className="mt-5 grid gap-3">
                    {queuePrompts.slice(0, 3).map((prompt) => (
                      <button
                        className={cn(
                          'flex items-center justify-between gap-3 rounded-2xl px-4 py-3 text-left transition hover:bg-white/[0.08]',
                          props.selectedPromptDetails?.id === prompt.id ? 'bg-violet-400/[0.16]' : 'bg-black/20',
                        )}
                        key={prompt.id}
                        onClick={(event) => {
                          event.stopPropagation();
                          props.onSelectPrompt(prompt.id);
                        }}
                        type="button"
                      >
                        <div>
                          <div className="font-bold text-slate-200">{prompt.title}</div>
                          <div className="mt-1 text-xs text-slate-500">#{prompt.position} / priority {prompt.priority}</div>
                        </div>
                        <StatusBadge status={prompt.status} />
                      </button>
                    ))}
                  </div>
                </article>
              );
            })}
          </div>
        </Panel>
      </section>

      {props.selectedPromptDetails && (
        <Panel className="mt-5">
          <PanelHeader
            eyebrow="Prompt result"
            title={props.selectedPromptDetails.title}
            description="Last saved execution response for this prompt. The response is loaded from prompt execution history."
            aside={<StatusBadge status={props.selectedPromptDetails.status} />}
          />

          <div className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">
              <div className="eyebrow">Prompt Content</div>
              <pre className="mt-3 max-h-80 overflow-auto whitespace-pre-wrap text-sm leading-6 text-slate-300">
                {props.selectedPromptDetails.content}
              </pre>
            </div>

            <div className="rounded-3xl border border-white/10 bg-black/20 p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="eyebrow">Codex Response</div>
                  <div className="mt-1 text-xs text-slate-500">
                    {props.selectedPromptDetails.lastExecution
                      ? `exit ${props.selectedPromptDetails.lastExecution.exitCode ?? 'n/a'} / ${props.selectedPromptDetails.lastExecution.durationMillis ?? 0} ms`
                      : 'No execution saved yet'}
                  </div>
                </div>
                {props.selectedPromptDetails.lastExecution && <StatusBadge status={props.selectedPromptDetails.lastExecution.status} />}
              </div>

              <pre className="mt-3 max-h-80 overflow-auto whitespace-pre-wrap text-sm leading-6 text-slate-200">
                {executionText(props.selectedPromptDetails)}
              </pre>
            </div>
          </div>
        </Panel>
      )}
    </>
  );
}

function executionText(prompt: PromptDetails) {
  const execution = prompt.lastExecution;
  if (!execution) {
    return 'No Codex response has been saved for this prompt yet.';
  }

  return execution.responseText
    ?? execution.errorMessage
    ?? execution.stderr
    ?? execution.rawOutput
    ?? 'Execution finished without text output.';
}
