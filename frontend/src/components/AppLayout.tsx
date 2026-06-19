import type { ReactNode } from 'react';
import { cn } from '../lib/cn';
import type { PageId } from '../types';

const navItems: Array<{ id: PageId; label: string; hint: string }> = [
  { id: 'dashboard', label: 'Dashboard', hint: 'Runtime overview' },
  { id: 'projects', label: 'Projects', hint: 'Local workspaces' },
  { id: 'ai-tools', label: 'AI Tools', hint: 'Codex and executors' },
  { id: 'queues', label: 'Queues', hint: 'Prompt pipelines' },
  { id: 'settings', label: 'Settings', hint: 'Runtime config' },
];

interface AppLayoutProps {
  activePage: PageId;
  onPageChange: (page: PageId) => void;
  selectedQueueName: string;
  selectedQueueStatus: string;
  children: ReactNode;
}

export function AppLayout(props: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-[#090b12] text-slate-100">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_left,rgba(124,92,255,0.28),transparent_30rem),radial-gradient(circle_at_80%_10%,rgba(35,211,171,0.16),transparent_24rem)]" />

      <div className="relative grid min-h-screen lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="sticky top-0 hidden h-screen border-r border-white/10 bg-slate-950/70 px-5 py-7 backdrop-blur-xl lg:block">
          <div className="mb-9 flex items-center gap-4">
            <div className="flex h-[52px] w-[52px] items-center justify-center rounded-2xl bg-gradient-to-br from-violet-500 to-emerald-300 font-black tracking-tight text-white shadow-2xl shadow-violet-700/30">
              VC
            </div>
            <div>
              <div className="text-lg font-black tracking-tight">Vibe Code</div>
              <div className="text-sm text-slate-400">Manager</div>
            </div>
          </div>

          <nav className="grid gap-2">
            {navItems.map((item) => (
              <button
                className={cn(
                  'rounded-2xl border px-4 py-3 text-left transition',
                  props.activePage === item.id
                    ? 'border-white/[0.12] bg-white/10 text-white shadow-lg shadow-black/20'
                    : 'border-transparent text-slate-300 hover:border-white/10 hover:bg-white/5 hover:text-white',
                )}
                key={item.id}
                onClick={() => props.onPageChange(item.id)}
                type="button"
              >
                <span className="block font-semibold">{item.label}</span>
                <span className="mt-0.5 block text-xs text-slate-500">{item.hint}</span>
              </button>
            ))}
          </nav>

          <div className="absolute bottom-6 left-5 right-5 rounded-3xl border border-white/10 bg-gradient-to-b from-violet-500/[0.15] to-white/5 p-5 shadow-2xl shadow-black/20">
            <div className="eyebrow">Selected Queue</div>
            <div className="mt-2 font-bold">{props.selectedQueueName}</div>
            <div className="mt-1 text-sm text-slate-400">{props.selectedQueueStatus}</div>
          </div>
        </aside>

        <main className="min-w-0 px-4 py-5 sm:px-6 lg:px-8 lg:py-8">
          <div className="mb-5 grid gap-2 lg:hidden">
            <div className="flex items-center justify-between rounded-3xl border border-white/10 bg-slate-950/70 p-3 backdrop-blur-xl">
              <div className="font-black">Vibe Code Manager</div>
              <div className="text-xs text-slate-400">{props.selectedQueueStatus}</div>
            </div>
            <div className="flex gap-2 overflow-x-auto pb-1">
              {navItems.map((item) => (
                <button
                  className={cn(
                    'shrink-0 rounded-2xl border px-4 py-2 text-sm',
                    props.activePage === item.id ? 'border-violet-400/50 bg-violet-500/20' : 'border-white/10 bg-white/5',
                  )}
                  key={item.id}
                  onClick={() => props.onPageChange(item.id)}
                  type="button"
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          {props.children}
        </main>
      </div>
    </div>
  );
}
