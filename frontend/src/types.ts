export type PageId = 'dashboard' | 'projects' | 'ai-tools' | 'queues' | 'settings';

export type ProjectStatus = 'ACTIVE' | 'DISABLED' | 'ARCHIVED';
export type AiToolType = 'FAKE' | 'CODEX' | 'CLAUDE_CODE' | 'CUSTOM';
export type AiToolStatus = 'ENABLED' | 'DISABLED';
export type LimitStatus = 'AVAILABLE' | 'LIMIT_REACHED' | 'UNKNOWN' | 'ERROR';
export type QueueStatus =
  | 'CREATED'
  | 'WAITING_LIMIT'
  | 'WAITING_CONFIRMATION'
  | 'RUNNING'
  | 'PAUSED'
  | 'STOPPED'
  | 'COMPLETED'
  | 'DISABLED';
export type PromptStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'SKIPPED';
export type AutoRunMode = 'NOTIFY_ONLY' | 'ASK_CONFIRMATION' | 'AUTO_RUN';

export interface Project {
  id: string;
  name: string;
  rootDirectory: string;
  status: ProjectStatus;
  queueCount: number;
  promptCount: number;
  lastActivity: string;
}

export interface AiTool {
  id: string;
  name: string;
  type: AiToolType;
  status: AiToolStatus;
  executablePath: string;
  limitStatus: LimitStatus;
  lastCheck: string;
}

export interface Prompt {
  id: string;
  queueId: string;
  title: string;
  status: PromptStatus;
  priority: number;
  position: number;
  toolName: string;
}

export interface Queue {
  id: string;
  projectId: string;
  name: string;
  status: QueueStatus;
  autoRunMode: AutoRunMode;
  maxPromptsPerRun: number;
  cooldownSeconds: number;
  stopOnError: boolean;
  queuedPrompts: number;
  completedPrompts: number;
  failedPrompts: number;
  nextPrompt: string | null;
  updatedAt: string;
}

export interface AppSettings {
  backendBaseUrl: string;
  mockMode: boolean;
  defaultExecutor: AiToolType;
  codexLimitCheck: boolean;
  notificationsEnabled: boolean;
  theme: 'dark' | 'system';
}

export interface DashboardSummary {
  projectsTotal: number;
  activeTools: number;
  runningQueues: number;
  queuedPrompts: number;
  waitingLimitQueues: number;
  completedToday: number;
}

export interface RunQueueResult {
  queueId: string;
  executedPrompts: number;
  stoppedOnError: boolean;
  reason: string;
}

export interface CreateProjectInput {
  name: string;
  rootDirectory: string;
}

export interface CreateAiToolInput {
  name: string;
  type: AiToolType;
  executablePath: string;
}

export interface CreateQueueInput {
  projectId: string;
  name: string;
  autoRunMode: AutoRunMode;
  maxPromptsPerRun: number;
  cooldownSeconds: number;
  stopOnError: boolean;
}

export interface CreatePromptInput {
  queueId: string;
  targetAiToolId: string;
  title: string;
  content: string;
  priority: number;
  maxAttempts: number;
  workingDirectoryOverride: string | null;
}
