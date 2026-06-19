import type { ReactNode } from 'react';
import { cn } from '../lib/cn';

type Tone = 'good' | 'info' | 'warn' | 'bad' | 'muted';

const toneClasses: Record<Tone, string> = {
  good: 'border-emerald-300/20 bg-emerald-400/10 text-emerald-200',
  info: 'border-blue-300/20 bg-blue-400/10 text-blue-200',
  warn: 'border-amber-300/20 bg-amber-400/10 text-amber-200',
  bad: 'border-rose-300/20 bg-rose-400/10 text-rose-200',
  muted: 'border-white/10 bg-white/[0.08] text-slate-300',
};

export function PageHeader(props: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <header className="mb-7 flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
      <div>
        <div className="eyebrow">{props.eyebrow}</div>
        <h1 className="mt-2 text-4xl font-black tracking-[-0.055em] text-white sm:text-5xl">{props.title}</h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-400 sm:text-base">{props.description}</p>
      </div>
      {props.actions && <div className="flex flex-wrap gap-3">{props.actions}</div>}
    </header>
  );
}

export function Button(props: {
  children: ReactNode;
  onClick?: () => void;
  type?: 'button' | 'submit';
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
}) {
  const variant = props.variant ?? 'ghost';

  return (
    <button
      className={cn(
        'rounded-2xl px-4 py-2.5 text-sm font-bold transition disabled:cursor-not-allowed disabled:opacity-50',
        variant === 'primary' && 'bg-gradient-to-r from-violet-500 to-blue-500 text-white shadow-xl shadow-violet-950/30',
        variant === 'secondary' && 'bg-emerald-400/[0.15] text-emerald-100 hover:bg-emerald-400/20',
        variant === 'ghost' && 'border border-white/10 bg-white/[0.07] text-slate-100 hover:bg-white/10',
        variant === 'danger' && 'bg-rose-400/[0.15] text-rose-100 hover:bg-rose-400/20',
      )}
      disabled={props.disabled}
      onClick={props.onClick}
      type={props.type ?? 'button'}
    >
      {props.children}
    </button>
  );
}

export function Panel(props: { children: ReactNode; className?: string }) {
  return (
    <section className={cn('rounded-[1.75rem] border border-white/10 bg-slate-900/70 p-5 shadow-2xl shadow-black/20 backdrop-blur-xl', props.className)}>
      {props.children}
    </section>
  );
}

export function PanelHeader(props: { eyebrow?: string; title: string; description?: string; aside?: ReactNode }) {
  return (
    <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        {props.eyebrow && <div className="eyebrow">{props.eyebrow}</div>}
        <h2 className="mt-1 text-2xl font-black tracking-tight text-white">{props.title}</h2>
        {props.description && <p className="mt-1 text-sm leading-6 text-slate-400">{props.description}</p>}
      </div>
      {props.aside}
    </div>
  );
}

export function StatCard(props: { label: string; value: string | number; caption: string; tone?: Tone }) {
  return (
    <article className="rounded-3xl border border-white/10 bg-slate-900/70 p-5 shadow-xl shadow-black/20">
      <div className="text-sm text-slate-400">{props.label}</div>
      <div className="mt-2 text-4xl font-black tracking-tight text-white">{props.value}</div>
      <div className={cn('mt-3 inline-flex rounded-full border px-3 py-1 text-xs font-bold', toneClasses[props.tone ?? 'muted'])}>{props.caption}</div>
    </article>
  );
}

export function StatusBadge(props: { status: string }) {
  return <span className={cn('inline-flex rounded-full border px-3 py-1 text-xs font-black uppercase tracking-wide', toneClasses[toneForStatus(props.status)])}>{props.status}</span>;
}

export function EmptyState(props: { title: string; description: string }) {
  return (
    <div className="flex min-h-36 items-center justify-center rounded-3xl border border-dashed border-white/[0.15] bg-white/[0.03] p-6 text-center">
      <div>
        <div className="font-bold text-slate-200">{props.title}</div>
        <div className="mt-1 text-sm text-slate-500">{props.description}</div>
      </div>
    </div>
  );
}

export function Field(props: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label className="grid gap-2 text-sm font-bold text-slate-300">
      {props.label}
      {props.children}
    </label>
  );
}

export const inputClassName =
  'w-full rounded-2xl border border-white/10 bg-white/[0.07] px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-violet-400/70 focus:ring-4 focus:ring-violet-500/10';

export function ProgressBar(props: { value: number }) {
  return (
    <div className="h-2 overflow-hidden rounded-full bg-white/[0.08]">
      <div className="h-full rounded-full bg-gradient-to-r from-violet-500 to-emerald-300" style={{ width: `${Math.min(100, Math.max(0, props.value))}%` }} />
    </div>
  );
}

function toneForStatus(status: string): Tone {
  const normalized = status.toLowerCase();
  if (['active', 'enabled', 'completed', 'available'].includes(normalized)) return 'good';
  if (['running', 'queued', 'created'].includes(normalized)) return 'info';
  if (['waiting_limit', 'waiting_confirmation', 'paused', 'unknown', 'limit_reached'].includes(normalized)) return 'warn';
  if (['failed', 'cancelled', 'stopped', 'disabled', 'archived', 'error'].includes(normalized)) return 'bad';
  return 'muted';
}
