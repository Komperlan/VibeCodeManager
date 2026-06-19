import { FormEvent, useEffect, useState } from 'react';
import { Button, Field, inputClassName, PageHeader, Panel, PanelHeader, StatusBadge } from '../components/ui';
import type { AiToolType, AppSettings } from '../types';

interface SettingsPageProps {
  settings: AppSettings | null;
  loading: boolean;
  onSave: (settings: AppSettings) => void;
}

export function SettingsPage(props: SettingsPageProps) {
  const [form, setForm] = useState<AppSettings | null>(props.settings);

  useEffect(() => {
    setForm(props.settings);
  }, [props.settings]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (form) {
      props.onSave(form);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Runtime Preferences"
        title="Settings"
        description="Frontend settings are mock-only for now. The API boundary is ready for a future Spring Boot settings endpoint."
      />

      <section className="grid gap-5 xl:grid-cols-[1fr_0.85fr]">
        <Panel>
          <PanelHeader eyebrow="Configuration" title="Application settings" />
          {form && (
            <form className="grid gap-5" onSubmit={handleSubmit}>
              <Field label="Backend base URL">
                <input
                  className={inputClassName}
                  value={form.backendBaseUrl}
                  onChange={(event) => setForm({ ...form, backendBaseUrl: event.target.value })}
                />
              </Field>

              <Field label="Default executor">
                <select
                  className={inputClassName}
                  value={form.defaultExecutor}
                  onChange={(event) => setForm({ ...form, defaultExecutor: event.target.value as AiToolType })}
                >
                  <option value="FAKE">FAKE</option>
                  <option value="CODEX">CODEX</option>
                  <option value="CLAUDE_CODE">CLAUDE_CODE</option>
                  <option value="CUSTOM">CUSTOM</option>
                </select>
              </Field>

              <Field label="Theme">
                <select className={inputClassName} value={form.theme} onChange={(event) => setForm({ ...form, theme: event.target.value as AppSettings['theme'] })}>
                  <option value="dark">Dark</option>
                  <option value="system">System</option>
                </select>
              </Field>

              <div className="grid gap-3 sm:grid-cols-3">
                <ToggleCard
                  checked={form.mockMode}
                  label="Mock API"
                  onChange={(checked) => setForm({ ...form, mockMode: checked })}
                />
                <ToggleCard
                  checked={form.codexLimitCheck}
                  label="Codex limit check"
                  onChange={(checked) => setForm({ ...form, codexLimitCheck: checked })}
                />
                <ToggleCard
                  checked={form.notificationsEnabled}
                  label="Notifications"
                  onChange={(checked) => setForm({ ...form, notificationsEnabled: checked })}
                />
              </div>

              <Button disabled={props.loading} type="submit" variant="primary">
                Save Settings
              </Button>
            </form>
          )}
        </Panel>

        <Panel>
          <PanelHeader eyebrow="Mode" title="API readiness" />
          <div className="grid gap-4">
            <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-5">
              <div className="mb-3 flex items-center justify-between gap-3">
                <div className="font-black text-white">Current data source</div>
                <StatusBadge status={form?.mockMode ? 'MOCK' : 'HTTP'} />
              </div>
              <p className="text-sm leading-6 text-slate-400">
                Set VITE_USE_MOCK_API=false to switch the API client to Spring Boot endpoints when the backend contract is ready.
              </p>
            </div>
            <div className="rounded-3xl border border-white/10 bg-black/20 p-5">
              <div className="eyebrow">Expected backend</div>
              <div className="mt-2 font-mono text-sm text-slate-300">/api/v1/projects, /api/v1/ai-tools, /api/v1/queues, /api/v1/prompts</div>
            </div>
          </div>
        </Panel>
      </section>
    </>
  );
}

function ToggleCard(props: { checked: boolean; label: string; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex cursor-pointer items-center justify-between gap-3 rounded-3xl border border-white/10 bg-white/[0.04] p-4">
      <span className="text-sm font-bold text-slate-200">{props.label}</span>
      <input
        checked={props.checked}
        className="h-5 w-5 accent-violet-500"
        onChange={(event) => props.onChange(event.target.checked)}
        type="checkbox"
      />
    </label>
  );
}
