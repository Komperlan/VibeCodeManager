import { Button, EmptyState, PageHeader, Panel, PanelHeader, ProgressBar, StatCard, StatusBadge } from '../components/ui';
import type { AiTool, DashboardSummary, Prompt, Queue, RunQueueResult } from '../types';

interface DashboardPageProps {
  summary: DashboardSummary | null;
  queues: Queue[];
  selectedQueue: Queue | undefined;
  prompts: Prompt[];
  tools: AiTool[];
  lastRun: RunQueueResult | null;
  loading: boolean;
  onRunQueue: (queueId: string) => void;
  onCheckLimits: () => void;
}

export function DashboardPage(props: DashboardPageProps) {
  const activeQueue = props.selectedQueue ?? props.queues.find((queue) => queue.status === 'RUNNING') ?? props.queues[0];
  const recentPrompts = props.prompts.slice(0, 5);

  return (
    <>
      <PageHeader
        eyebrow="Local-first AI queue orchestration"
        title="Dashboard"
        description="A compact control room for projects, prompt queues, AI tools, Codex limit checks, and the next runnable work item."
        actions={
          <>
            <Button disabled={props.loading} onClick={props.onCheckLimits} variant="ghost">
              Check Limits
            </Button>
            <Button disabled={!activeQueue || props.loading} onClick={() => activeQueue && props.onRunQueue(activeQueue.id)} variant="primary">
              Run Active Queue
            </Button>
          </>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Projects" value={props.summary?.projectsTotal ?? 0} caption="tracked locally" tone="info" />
        <StatCard label="AI Tools" value={props.summary?.activeTools ?? 0} caption="enabled executors" tone="good" />
        <StatCard label="Queues" value={props.summary?.runningQueues ?? 0} caption="running now" tone="info" />
        <StatCard label="Prompts" value={props.summary?.queuedPrompts ?? 0} caption="waiting in queue" tone="warn" />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.35fr_0.85fr]">
        <Panel>
          <PanelHeader
            eyebrow="Run Control"
            title={activeQueue?.name ?? 'No queue selected'}
            description="Manual queue execution remains explicit. The backend will check limits before it starts a prompt."
            aside={activeQueue && <StatusBadge status={activeQueue.status} />}
          />

          {activeQueue ? (
            <div className="grid gap-5">
              <div className="rounded-3xl border border-white/10 bg-gradient-to-br from-violet-500/[0.15] to-emerald-400/10 p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <div className="text-lg font-black text-white">{activeQueue.nextPrompt ?? 'No prompt selected'}</div>
                    <div className="mt-2 text-sm text-slate-400">
                      {activeQueue.autoRunMode} / max {activeQueue.maxPromptsPerRun} prompts / cooldown {activeQueue.cooldownSeconds}s
                    </div>
                  </div>
                  <Button disabled={props.loading} onClick={() => props.onRunQueue(activeQueue.id)} variant="secondary">
                    Run Queue
                  </Button>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-3">
                <QueueMiniStat label="Queued" value={activeQueue.queuedPrompts} />
                <QueueMiniStat label="Completed" value={activeQueue.completedPrompts} />
                <QueueMiniStat label="Failed" value={activeQueue.failedPrompts} />
              </div>

              {props.lastRun && (
                <div className="rounded-3xl border border-white/10 bg-black/20 p-4">
                  <div className="eyebrow">Last Run</div>
                  <div className="mt-2 text-sm text-slate-300">
                    Executed {props.lastRun.executedPrompts} prompts. {props.lastRun.reason}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <EmptyState title="No queues yet" description="Create a project queue to start running prompts." />
          )}
        </Panel>

        <Panel>
          <PanelHeader eyebrow="Runtime" title="Executor Health" />
          <div className="grid gap-3">
            {props.tools.length === 0 && <EmptyState title="No executors" description="Register an AI tool before running queues." />}
            {props.tools.map((tool) => (
              <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-4" key={tool.id}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="font-bold text-white">{tool.name}</div>
                    <div className="mt-1 text-sm text-slate-500">{tool.type} / {tool.executablePath}</div>
                  </div>
                  <StatusBadge status={tool.limitStatus} />
                </div>
                <div className="mt-4">
                  <ProgressBar value={tool.limitStatus === 'AVAILABLE' ? 84 : tool.limitStatus === 'LIMIT_REACHED' ? 18 : 42} />
                </div>
                <div className="mt-2 text-xs text-slate-500">Last check: {tool.lastCheck}</div>
              </div>
            ))}
          </div>
        </Panel>
      </section>

      <section className="mt-5 grid gap-5 xl:grid-cols-2">
        <Panel>
          <PanelHeader eyebrow="Next Up" title="Prompt Backlog" />
          <div className="grid gap-3">
            {recentPrompts.length === 0 && <EmptyState title="No prompts queued" description="Add prompts to a queue to see the next runnable work." />}
            {recentPrompts.map((prompt) => (
              <div className="flex items-center justify-between gap-4 rounded-3xl border border-white/10 bg-white/[0.04] p-4" key={prompt.id}>
                <div className="flex items-center gap-4">
                  <div className="flex h-11 min-w-14 items-center justify-center rounded-2xl bg-violet-400/[0.15] font-black text-violet-100">#{prompt.position}</div>
                  <div>
                    <div className="font-bold text-white">{prompt.title}</div>
                    <div className="mt-1 text-sm text-slate-500">priority {prompt.priority} / {prompt.toolName}</div>
                  </div>
                </div>
                <StatusBadge status={prompt.status} />
              </div>
            ))}
          </div>
        </Panel>

        <Panel>
          <PanelHeader eyebrow="Limits" title="Codex Limit Checker" />
          <div className="rounded-3xl border border-amber-300/20 bg-amber-300/10 p-5">
            <div className="text-lg font-black text-amber-100">{props.summary?.waitingLimitQueues ?? 0} queues waiting for limit</div>
            <p className="mt-2 text-sm leading-6 text-amber-100/70">
              The v1 checker probes Codex before prompt execution. If the limit is not available, the queue moves to WAITING_LIMIT and the prompt stays queued.
            </p>
          </div>
        </Panel>
      </section>
    </>
  );
}

function QueueMiniStat(props: { label: string; value: number }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-4">
      <div className="text-sm text-slate-500">{props.label}</div>
      <div className="mt-1 text-3xl font-black text-white">{props.value}</div>
    </div>
  );
}
