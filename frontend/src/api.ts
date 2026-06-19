import { mockAiTools, mockProjects, mockPrompts, mockQueues, mockSettings } from './data/mockData';
import type {
  AiTool,
  AiToolStatus,
  AiToolType,
  AppSettings,
  CreateAiToolInput,
  CreatePromptInput,
  CreateProjectInput,
  CreateQueueInput,
  DashboardSummary,
  Project,
  ProjectStatus,
  Prompt,
  PromptStatus,
  Queue,
  QueueStatus,
  RunQueueResult,
} from './types';

export interface ApiClient {
  getDashboardSummary(): Promise<DashboardSummary>;
  listProjects(): Promise<Project[]>;
  createProject(input: CreateProjectInput): Promise<void>;
  listAiTools(): Promise<AiTool[]>;
  createAiTool(input: CreateAiToolInput): Promise<void>;
  listQueues(): Promise<Queue[]>;
  createQueue(input: CreateQueueInput): Promise<void>;
  listPrompts(): Promise<Prompt[]>;
  createPrompt(input: CreatePromptInput): Promise<void>;
  getSettings(): Promise<AppSettings>;
  updateSettings(settings: AppSettings): Promise<AppSettings>;
  runQueue(queueId: string): Promise<RunQueueResult>;
  checkLimits(): Promise<AiTool[]>;
}

let settingsState = { ...mockSettings };
let projectsState = [...mockProjects];
let toolsState = [...mockAiTools];
let queuesState = [...mockQueues];
let promptsState = [...mockPrompts];

function delay<T>(value: T) {
  return new Promise<T>((resolve) => {
    window.setTimeout(() => resolve(value), 180);
  });
}

const mockApi: ApiClient = {
  getDashboardSummary: () =>
    delay({
      projectsTotal: projectsState.length,
      activeTools: toolsState.filter((tool) => tool.status === 'ENABLED').length,
      runningQueues: queuesState.filter((queue) => queue.status === 'RUNNING').length,
      queuedPrompts: promptsState.filter((prompt) => prompt.status === 'QUEUED').length,
      waitingLimitQueues: queuesState.filter((queue) => queue.status === 'WAITING_LIMIT').length,
      completedToday: queuesState.reduce((sum, queue) => sum + queue.completedPrompts, 0),
    }),
  listProjects: () => delay(projectsState.map(withProjectStats)),
  createProject: (input) => {
    projectsState = [
      ...projectsState,
      {
        id: nextClientId('project'),
        name: input.name,
        rootDirectory: input.rootDirectory,
        status: 'ACTIVE',
        queueCount: 0,
        promptCount: 0,
        lastActivity: 'Just now',
      },
    ];
    return delay(undefined);
  },
  listAiTools: () => delay([...toolsState]),
  createAiTool: (input) => {
    toolsState = [
      ...toolsState,
      {
        id: nextClientId('tool'),
        name: input.name,
        type: input.type,
        status: 'ENABLED',
        executablePath: input.executablePath,
        limitStatus: input.type === 'FAKE' ? 'AVAILABLE' : 'UNKNOWN',
        lastCheck: 'Not checked',
      },
    ];
    return delay(undefined);
  },
  listQueues: () => delay([...queuesState]),
  createQueue: (input) => {
    queuesState = [
      ...queuesState,
      {
        id: nextClientId('queue'),
        projectId: input.projectId,
        name: input.name,
        status: 'CREATED',
        autoRunMode: input.autoRunMode,
        maxPromptsPerRun: input.maxPromptsPerRun,
        cooldownSeconds: input.cooldownSeconds,
        stopOnError: input.stopOnError,
        queuedPrompts: 0,
        completedPrompts: 0,
        failedPrompts: 0,
        nextPrompt: null,
        updatedAt: 'Just now',
      },
    ];
    return delay(undefined);
  },
  listPrompts: () => delay([...promptsState]),
  createPrompt: (input) => {
    const targetTool = toolsState.find((tool) => tool.id === input.targetAiToolId);
    const nextPosition = nextPromptPosition(input.queueId);
    promptsState = [
      ...promptsState,
      {
        id: nextClientId('prompt'),
        queueId: input.queueId,
        title: input.title,
        status: 'QUEUED',
        priority: input.priority,
        position: nextPosition,
        toolName: targetTool?.name ?? 'Unknown tool',
      },
    ];
    queuesState = queuesState.map((queue) => {
      if (queue.id !== input.queueId) {
        return queue;
      }

      return {
        ...queue,
        queuedPrompts: queue.queuedPrompts + 1,
        nextPrompt: queue.nextPrompt ?? input.title,
        updatedAt: 'Just now',
      };
    });

    return delay(undefined);
  },
  getSettings: () => delay({ ...settingsState }),
  updateSettings: (settings) => {
    settingsState = { ...settings };
    return delay({ ...settingsState });
  },
  runQueue: (queueId) =>
    delay({
      queueId,
      executedPrompts: queueId === 'queue-docs' ? 0 : 2,
      stoppedOnError: false,
      reason: queueId === 'queue-docs' ? 'Queue is waiting for Codex limit reset' : 'Mock run completed',
    }),
  checkLimits: () => {
    toolsState = toolsState.map((tool) => ({
      ...tool,
      lastCheck: 'Just now',
      limitStatus: tool.status === 'DISABLED' ? 'UNKNOWN' : 'AVAILABLE',
    }));
    return delay([...toolsState]);
  },
};

function nextClientId(prefix: string) {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return `${prefix}-${crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now()}`;
}

function withProjectStats(project: Project): Project {
  const projectQueues = queuesState.filter((queue) => queue.projectId === project.id);
  const projectQueueIds = new Set(projectQueues.map((queue) => queue.id));

  return {
    ...project,
    queueCount: projectQueues.length,
    promptCount: promptsState.filter((prompt) => projectQueueIds.has(prompt.queueId)).length,
  };
}

function nextPromptPosition(queueId: string) {
  const positions = promptsState
    .filter((prompt) => prompt.queueId === queueId)
    .map((prompt) => prompt.position);

  return positions.length === 0 ? 1 : Math.max(...positions) + 1;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

interface ProjectSummaryResponse {
  id: string;
  name: string;
  status: ProjectStatus;
}

interface AiToolSummaryResponse {
  id: string;
  name: string;
  type: AiToolType;
  status: AiToolStatus;
}

interface QueueSummaryResponse {
  id: string;
  name: string;
  status: QueueStatus;
}

interface PromptSummaryResponse {
  id: string;
  title: string;
  status: PromptStatus;
  priority: number;
  position: number;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
    ...init,
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function errorMessage(response: Response): Promise<string> {
  const fallback = `Request failed: ${response.status}`;
  const contentType = response.headers.get('content-type') ?? '';

  if (!contentType.includes('application/json')) {
    return fallback;
  }

  try {
    const body = (await response.json()) as { code?: string; message?: string };
    const details = [body.code, body.message].filter(Boolean).join(' - ');
    return details ? `${fallback} (${details})` : fallback;
  } catch {
    return fallback;
  }
}

async function listHttpProjects(): Promise<Project[]> {
  const projects = await request<ProjectSummaryResponse[]>('/api/v1/projects');

  return projects.map((project) => ({
    id: project.id,
    name: project.name,
    status: project.status,
    rootDirectory: 'Load project details to show root directory',
    queueCount: 0,
    promptCount: 0,
    lastActivity: 'From backend',
  }));
}

async function listHttpAiTools(): Promise<AiTool[]> {
  const tools = await request<AiToolSummaryResponse[]>('/api/v1/ai-tools');

  return tools.map((tool) => ({
    id: tool.id,
    name: tool.name,
    type: tool.type,
    status: tool.status,
    executablePath: 'Load tool details to show executable path',
    limitStatus: tool.status === 'ENABLED' ? 'UNKNOWN' : 'ERROR',
    lastCheck: 'Not checked in this view',
  }));
}

async function listHttpQueues(projects?: Project[]): Promise<Queue[]> {
  const sourceProjects = projects ?? (await listHttpProjects());
  const queuesByProject = await Promise.all(
    sourceProjects.map(async (project) => {
      const queues = await request<QueueSummaryResponse[]>(`/api/v1/queues?projectId=${encodeURIComponent(project.id)}`);

      return queues.map((queue) => ({
        id: queue.id,
        projectId: project.id,
        name: queue.name,
        status: queue.status,
        autoRunMode: 'ASK_CONFIRMATION' as const,
        maxPromptsPerRun: 1,
        cooldownSeconds: 0,
        stopOnError: true,
        queuedPrompts: 0,
        completedPrompts: 0,
        failedPrompts: 0,
        nextPrompt: null,
        updatedAt: 'From backend',
      }));
    }),
  );

  return queuesByProject.flat();
}

async function listHttpPrompts(queues?: Queue[]): Promise<Prompt[]> {
  const sourceQueues = queues ?? (await listHttpQueues());
  const promptsByQueue = await Promise.all(
    sourceQueues.map(async (queue) => {
      const prompts = await request<PromptSummaryResponse[]>(`/api/v1/prompts?queueId=${encodeURIComponent(queue.id)}`);

      return prompts.map((prompt) => ({
        id: prompt.id,
        queueId: queue.id,
        title: prompt.title,
        status: prompt.status,
        priority: prompt.priority,
        position: prompt.position,
        toolName: 'From backend',
      }));
    }),
  );

  return promptsByQueue.flat();
}

const httpApi: ApiClient = {
  getDashboardSummary: async () => {
    const [projects, tools] = await Promise.all([listHttpProjects(), listHttpAiTools()]);
    const queues = await listHttpQueues(projects);
    const prompts = await listHttpPrompts(queues);

    return {
      projectsTotal: projects.length,
      activeTools: tools.filter((tool) => tool.status === 'ENABLED').length,
      runningQueues: queues.filter((queue) => queue.status === 'RUNNING').length,
      queuedPrompts: prompts.filter((prompt) => prompt.status === 'QUEUED').length,
      waitingLimitQueues: queues.filter((queue) => queue.status === 'WAITING_LIMIT').length,
      completedToday: queues.reduce((sum, queue) => sum + queue.completedPrompts, 0),
    };
  },
  listProjects: listHttpProjects,
  createProject: (input) =>
    request<void>('/api/v1/projects', {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  listAiTools: listHttpAiTools,
  createAiTool: (input) =>
    request<void>('/api/v1/ai-tools', {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  listQueues: () => listHttpQueues(),
  createQueue: (input) =>
    request<void>('/api/v1/queues', {
      method: 'POST',
      body: JSON.stringify({
        projectId: input.projectId,
        name: input.name,
        executionPolicy: {
          autoRunMode: input.autoRunMode,
          maxPromptsPerRun: input.maxPromptsPerRun,
          cooldown: durationFromSeconds(input.cooldownSeconds),
          stopOnError: input.stopOnError,
          workingHoursEnabled: false,
          workingHours: null,
        },
      }),
    }),
  listPrompts: () => listHttpPrompts(),
  createPrompt: (input) =>
    request<void>('/api/v1/prompts', {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  getSettings: async () => ({
    ...settingsState,
    backendBaseUrl: API_BASE_URL || 'http://127.0.0.1:8080',
    mockMode: false,
  }),
  updateSettings: async (settings) => {
    settingsState = { ...settings };
    return { ...settingsState };
  },
  runQueue: (queueId) =>
    request<RunQueueResult>(`/api/v1/queues/${queueId}/runner/run`, {
      method: 'POST',
      body: JSON.stringify({ maxPrompts: 3 }),
    }),
  checkLimits: async () => {
    const tools = await listHttpAiTools();
    return tools.map((tool) => ({
      ...tool,
      lastCheck: 'Limit check endpoint is not implemented yet',
      limitStatus: tool.status === 'ENABLED' ? 'UNKNOWN' : 'ERROR',
    }));
  },
};

function durationFromSeconds(seconds: number) {
  return `PT${Math.max(0, Math.trunc(seconds))}S`;
}

export const api: ApiClient = import.meta.env.VITE_USE_MOCK_API === 'false' ? httpApi : mockApi;
