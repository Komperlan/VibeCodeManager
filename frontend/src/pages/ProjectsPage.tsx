import { type FormEvent, useState } from 'react';
import { Button, EmptyState, Field, inputClassName, PageHeader, Panel, PanelHeader, StatusBadge } from '../components/ui';
import type { CreateProjectInput, Project } from '../types';

interface ProjectsPageProps {
  projects: Project[];
  selectedProjectId: string | null;
  loading: boolean;
  onCreateProject: (input: CreateProjectInput) => void;
  onSelectProject: (projectId: string) => void;
}

export function ProjectsPage(props: ProjectsPageProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState<CreateProjectInput>({
    name: '',
    rootDirectory: '',
  });
  const selectedProject = props.projects.find((project) => project.id === props.selectedProjectId) ?? props.projects[0];

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    props.onCreateProject({
      name: form.name.trim(),
      rootDirectory: form.rootDirectory.trim(),
    });
    setForm({ name: '', rootDirectory: '' });
    setShowCreateForm(false);
  }

  return (
    <>
      <PageHeader
        eyebrow="Workspace Roots"
        title="Projects"
        description="Each project points to a local root directory. Queues and prompts inherit that workspace unless a prompt overrides it."
        actions={
          <Button disabled={props.loading} onClick={() => setShowCreateForm((visible) => !visible)} variant="primary">
            {showCreateForm ? 'Close Form' : 'New Project'}
          </Button>
        }
      />

      {showCreateForm && (
        <Panel className="mb-5">
          <PanelHeader eyebrow="Create" title="New project" description="Use an absolute local root directory for the repository you want to automate." />
          <form className="grid gap-4 lg:grid-cols-[1fr_1.4fr_auto]" onSubmit={handleSubmit}>
            <Field label="Name">
              <input
                className={inputClassName}
                maxLength={100}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Vibe Code Manager"
                required
                value={form.name}
              />
            </Field>
            <Field label="Root directory">
              <input
                className={inputClassName}
                onChange={(event) => setForm((current) => ({ ...current, rootDirectory: event.target.value }))}
                placeholder="/home/dryu/gits/VibeCodeManager"
                required
                value={form.rootDirectory}
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

      <section className="grid gap-5 xl:grid-cols-[0.9fr_1.1fr]">
        <Panel>
          <PanelHeader eyebrow="Registered" title="Local projects" />
          <div className="grid gap-3">
            {props.projects.length === 0 && <EmptyState title="No projects" description="Create a project to bind queues to a local repository." />}
            {props.projects.map((project) => (
              <button
                className="rounded-3xl border border-white/10 bg-white/[0.04] p-4 text-left transition hover:border-violet-300/30 hover:bg-white/[0.07]"
                key={project.id}
                onClick={() => props.onSelectProject(project.id)}
                type="button"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="font-black text-white">{project.name}</div>
                    <div className="mt-1 break-all text-sm text-slate-500">{project.rootDirectory}</div>
                  </div>
                  <StatusBadge status={project.status} />
                </div>
                <div className="mt-4 grid grid-cols-3 gap-3 text-sm">
                  <SmallStat label="Queues" value={project.queueCount} />
                  <SmallStat label="Prompts" value={project.promptCount} />
                  <SmallStat label="Updated" value={project.lastActivity} />
                </div>
              </button>
            ))}
          </div>
        </Panel>

        <Panel>
          <PanelHeader eyebrow="Details" title={selectedProject?.name ?? 'Select a project'} />
          {selectedProject ? (
            <div className="grid gap-4">
              <div className="rounded-3xl border border-white/10 bg-black/20 p-5">
                <div className="eyebrow">Root Directory</div>
                <div className="mt-2 break-all font-mono text-sm text-slate-200">{selectedProject.rootDirectory}</div>
              </div>
              <div className="grid gap-4 md:grid-cols-3">
                <InfoCard label="Status" value={selectedProject.status} />
                <InfoCard label="Queues" value={selectedProject.queueCount} />
                <InfoCard label="Prompts" value={selectedProject.promptCount} />
              </div>
              <div className="flex flex-wrap gap-3">
                <Button variant="ghost">Rename</Button>
                <Button variant="ghost">Change Root</Button>
                <Button variant="danger">Archive</Button>
              </div>
            </div>
          ) : (
            <EmptyState title="Nothing selected" description="Choose a project from the list." />
          )}
        </Panel>
      </section>
    </>
  );
}

function SmallStat(props: { label: string; value: string | number }) {
  return (
    <div className="rounded-2xl bg-white/[0.04] p-3">
      <div className="text-xs text-slate-500">{props.label}</div>
      <div className="mt-1 truncate font-bold text-slate-200">{props.value}</div>
    </div>
  );
}

function InfoCard(props: { label: string; value: string | number }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-4">
      <div className="text-sm text-slate-500">{props.label}</div>
      <div className="mt-2 text-xl font-black text-white">{props.value}</div>
    </div>
  );
}
