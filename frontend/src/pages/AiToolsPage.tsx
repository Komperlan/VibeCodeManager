import { type FormEvent, useState } from 'react';
import { Button, EmptyState, Field, inputClassName, PageHeader, Panel, PanelHeader, StatusBadge } from '../components/ui';
import type { AiTool, AiToolType, CreateAiToolInput } from '../types';

interface AiToolsPageProps {
  tools: AiTool[];
  loading: boolean;
  onCheckLimits: () => void;
  onCreateAiTool: (input: CreateAiToolInput) => void;
}

export function AiToolsPage(props: AiToolsPageProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState<CreateAiToolInput>({
    name: '',
    type: 'CODEX',
    executablePath: 'codex',
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    props.onCreateAiTool({
      name: form.name.trim(),
      type: form.type,
      executablePath: form.executablePath.trim(),
    });
    setForm({ name: '', type: 'CODEX', executablePath: 'codex' });
    setShowCreateForm(false);
  }

  return (
    <>
      <PageHeader
        eyebrow="Executor Registry"
        title="AI Tools"
        description="Register local executors once, then route prompts to Codex, Claude Code, custom CLI tools, or the fake executor for tests."
        actions={
          <>
            <Button disabled={props.loading} onClick={props.onCheckLimits} variant="ghost">
              Refresh Limits
            </Button>
            <Button disabled={props.loading} onClick={() => setShowCreateForm((visible) => !visible)} variant="primary">
              {showCreateForm ? 'Close Form' : 'Add Tool'}
            </Button>
          </>
        }
      />

      {showCreateForm && (
        <Panel className="mb-5">
          <PanelHeader eyebrow="Create" title="Add AI tool" description="Register a local executable that future prompts can target." />
          <form className="grid gap-4 xl:grid-cols-[1fr_0.7fr_1.3fr_auto]" onSubmit={handleSubmit}>
            <Field label="Name">
              <input
                className={inputClassName}
                maxLength={100}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Codex CLI"
                required
                value={form.name}
              />
            </Field>
            <Field label="Type">
              <select
                className={inputClassName}
                onChange={(event) => setForm((current) => ({ ...current, type: event.target.value as AiToolType }))}
                value={form.type}
              >
                <option value="CODEX">CODEX</option>
                <option value="FAKE">FAKE</option>
                <option value="CLAUDE_CODE">CLAUDE_CODE</option>
                <option value="CUSTOM">CUSTOM</option>
              </select>
            </Field>
            <Field label="Executable path">
              <input
                className={inputClassName}
                onChange={(event) => setForm((current) => ({ ...current, executablePath: event.target.value }))}
                placeholder="codex"
                required
                value={form.executablePath}
              />
            </Field>
            <div className="flex items-end">
              <Button disabled={props.loading} type="submit" variant="primary">
                Create
              </Button>
            </div>
          </form>
        </Panel>
      )}

      <section className="grid gap-5 xl:grid-cols-[1fr_0.85fr]">
        <Panel>
          <PanelHeader eyebrow="Configured" title="Executors" />
          <div className="grid gap-4">
            {props.tools.length === 0 && <EmptyState title="No AI tools" description="Register Codex, Claude Code, a custom CLI, or the fake executor." />}
            {props.tools.map((tool) => (
              <article className="rounded-3xl border border-white/10 bg-white/[0.04] p-5" key={tool.id}>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="text-xl font-black text-white">{tool.name}</div>
                    <div className="mt-1 text-sm text-slate-500">{tool.type}</div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <StatusBadge status={tool.status} />
                    <StatusBadge status={tool.limitStatus} />
                  </div>
                </div>
                <div className="mt-5 rounded-2xl bg-black/20 p-4">
                  <div className="text-xs font-bold uppercase tracking-wide text-slate-500">Executable</div>
                  <div className="mt-1 break-all font-mono text-sm text-slate-200">{tool.executablePath}</div>
                </div>
                <div className="mt-4 flex items-center justify-between gap-3 text-sm text-slate-500">
                  <span>Last limit check: {tool.lastCheck}</span>
                  <Button variant={tool.status === 'ENABLED' ? 'danger' : 'secondary'}>
                    {tool.status === 'ENABLED' ? 'Disable' : 'Enable'}
                  </Button>
                </div>
              </article>
            ))}
          </div>
        </Panel>

        <Panel>
          <PanelHeader eyebrow="Implementation Notes" title="Executor contract" />
          <div className="grid gap-4 text-sm leading-6 text-slate-400">
            <p>Every executor should receive a normalized prompt execution request from the application layer.</p>
            <p>CLI executors should use ProcessRunner, pass arguments without shell wrapping, stream stdout and stderr safely, and respect timeout/output limits.</p>
            <p>Limit checkers run before execution. For Codex v1 the checker uses a tiny probe prompt and maps limit messages to WAITING_LIMIT.</p>
          </div>
        </Panel>
      </section>
    </>
  );
}
