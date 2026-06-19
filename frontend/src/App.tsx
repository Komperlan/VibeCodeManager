import { useEffect, useMemo, useState } from 'react';
import { api } from './api';
import { AppLayout } from './components/AppLayout';
import { Button, PageHeader } from './components/ui';
import { AiToolsPage } from './pages/AiToolsPage';
import { DashboardPage } from './pages/DashboardPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { QueuesPage } from './pages/QueuesPage';
import { SettingsPage } from './pages/SettingsPage';
import type {
  AiTool,
  AppSettings,
  CreateAiToolInput,
  CreatePromptInput,
  CreateProjectInput,
  CreateQueueInput,
  DashboardSummary,
  PageId,
  Project,
  Prompt,
  Queue,
  RunQueueResult,
} from './types';

export function App() {
  const [activePage, setActivePage] = useState<PageId>('dashboard');
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [tools, setTools] = useState<AiTool[]>([]);
  const [queues, setQueues] = useState<Queue[]>([]);
  const [prompts, setPrompts] = useState<Prompt[]>([]);
  const [settings, setSettings] = useState<AppSettings | null>(null);
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [lastRun, setLastRun] = useState<RunQueueResult | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const selectedQueue = useMemo(
    () => queues.find((queue) => queue.status === 'RUNNING') ?? queues.find((queue) => queue.projectId === selectedProjectId) ?? queues[0],
    [queues, selectedProjectId],
  );

  useEffect(() => {
    void loadData();
  }, []);

  async function loadData() {
    await runAction(refreshWorkspace);
  }

  async function refreshWorkspace() {
    const [nextSummary, nextProjects, nextTools, nextQueues, nextPrompts, nextSettings] = await Promise.all([
      api.getDashboardSummary(),
      api.listProjects(),
      api.listAiTools(),
      api.listQueues(),
      api.listPrompts(),
      api.getSettings(),
    ]);

    setSummary(nextSummary);
    setProjects(nextProjects);
    setTools(nextTools);
    setQueues(nextQueues);
    setPrompts(nextPrompts);
    setSettings(nextSettings);
    setSelectedProjectId((currentProjectId) => {
      if (currentProjectId && nextProjects.some((project) => project.id === currentProjectId)) {
        return currentProjectId;
      }

      return nextProjects[0]?.id ?? null;
    });
  }

  async function runAction(action: () => Promise<void>) {
    setLoading(true);
    setError(null);
    try {
      await action();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unexpected frontend error');
    } finally {
      setLoading(false);
    }
  }

  function handleSelectProject(projectId: string) {
    setSelectedProjectId(projectId || null);
  }

  function handleRunQueue(queueId: string) {
    void runAction(async () => {
      const result = await api.runQueue(queueId);
      setLastRun(result);
      setNotice(result.reason);
    });
  }

  function handleCreateProject(input: CreateProjectInput) {
    void runAction(async () => {
      await api.createProject(input);
      await refreshWorkspace();
      setNotice(`Project "${input.name}" created`);
    });
  }

  function handleCreateAiTool(input: CreateAiToolInput) {
    void runAction(async () => {
      await api.createAiTool(input);
      await refreshWorkspace();
      setNotice(`AI tool "${input.name}" created`);
    });
  }

  function handleCreateQueue(input: CreateQueueInput) {
    void runAction(async () => {
      await api.createQueue(input);
      await refreshWorkspace();
      setSelectedProjectId(input.projectId);
      setNotice(`Queue "${input.name}" created`);
    });
  }

  function handleCreatePrompt(input: CreatePromptInput) {
    void runAction(async () => {
      await api.createPrompt(input);
      await refreshWorkspace();
      const targetQueue = queues.find((queue) => queue.id === input.queueId);
      if (targetQueue) {
        setSelectedProjectId(targetQueue.projectId);
      }
      setNotice(`Prompt "${input.title}" added`);
    });
  }

  function handleCheckLimits() {
    void runAction(async () => {
      const nextTools = await api.checkLimits();
      const nextSummary = await api.getDashboardSummary();
      setTools(nextTools);
      setSummary(nextSummary);
      setNotice('Limit statuses refreshed');
    });
  }

  function handleSaveSettings(nextSettings: AppSettings) {
    void runAction(async () => {
      const saved = await api.updateSettings(nextSettings);
      setSettings(saved);
      setNotice('Settings saved in mock state');
    });
  }

  return (
    <AppLayout
      activePage={activePage}
      onPageChange={setActivePage}
      selectedQueueName={selectedQueue?.name ?? 'No queue selected'}
      selectedQueueStatus={selectedQueue?.status ?? 'Create a queue to start'}
    >
      <div className="mx-auto max-w-[1480px]">
        {(notice || error || loading) && (
          <section className="mb-5 grid gap-3">
            {loading && <div className="rounded-2xl border border-white/10 bg-white/[0.07] px-4 py-3 text-sm text-slate-300">Loading mock workspace...</div>}
            {notice && !error && <div className="rounded-2xl border border-emerald-300/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-100">{notice}</div>}
            {error && <div className="rounded-2xl border border-rose-300/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">{error}</div>}
          </section>
        )}

        {activePage === 'dashboard' && (
          <DashboardPage
            summary={summary}
            queues={queues}
            prompts={prompts}
            tools={tools}
            lastRun={lastRun}
            loading={loading}
            onRunQueue={handleRunQueue}
            onCheckLimits={handleCheckLimits}
          />
        )}

        {activePage === 'projects' && (
          <ProjectsPage
            projects={projects}
            selectedProjectId={selectedProjectId}
            loading={loading}
            onCreateProject={handleCreateProject}
            onSelectProject={handleSelectProject}
          />
        )}

        {activePage === 'ai-tools' && (
          <AiToolsPage
            tools={tools}
            loading={loading}
            onCheckLimits={handleCheckLimits}
            onCreateAiTool={handleCreateAiTool}
          />
        )}

        {activePage === 'queues' && (
          <QueuesPage
            projects={projects}
            tools={tools}
            queues={queues}
            prompts={prompts}
            selectedProjectId={selectedProjectId}
            onSelectProject={handleSelectProject}
            loading={loading}
            onCreateQueue={handleCreateQueue}
            onCreatePrompt={handleCreatePrompt}
            onRunQueue={handleRunQueue}
          />
        )}

        {activePage === 'settings' && <SettingsPage settings={settings} loading={loading} onSave={handleSaveSettings} />}

        {activePage !== 'dashboard' && (
          <div className="mt-7">
            <PageHeader
              eyebrow="Fast Action"
              title="Run from anywhere"
              description="The selected queue stays visible in the sidebar, so the control surface remains close even outside the dashboard."
              actions={
                <>
                  <Button disabled={loading} onClick={() => void loadData()} variant="ghost">
                    Refresh
                  </Button>
                  <Button disabled={!selectedQueue || loading} onClick={() => selectedQueue && handleRunQueue(selectedQueue.id)} variant="primary">
                    Run Selected Queue
                  </Button>
                </>
              }
            />
          </div>
        )}
      </div>
    </AppLayout>
  );
}
