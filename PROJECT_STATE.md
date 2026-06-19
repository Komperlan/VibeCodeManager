# Vibe Code Manager / текущее состояние проекта

Снимок состояния на 2026-06-18.

Этот файл фиксирует актуальную техническую картину проекта: что уже реализовано,
как устроены модули, какие проверки проходят и что логично делать дальше.
Источник истины по поведению - код и тесты.

## 1. Коротко

Vibe Code Manager - local-first приложение для управления проектами, AI tools,
очередями промптов и запуском этих промптов через executor adapters.

Сейчас проект состоит из:

- backend на Java 21 / Spring Boot 4 / Maven multi-module;
- PostgreSQL persistence через Spring Data JPA и Flyway;
- REST API для projects, AI tools, queues, prompts и queue runner;
- fake executor для разработки и тестов;
- Codex CLI executor через безопасный `ProcessRunner`;
- Codex limit checker v1 через probe-запуск;
- React + TypeScript frontend по Figma Make макету с mock data;
- Docker Compose с PostgreSQL по умолчанию и demo-профилем для backend/frontend
  на fake executor;
- Swagger/OpenAPI для ручной проверки API.

## 2. Архитектура

Backend построен как модульный монолит в стиле DDD + Hexagonal Architecture.

Направление зависимостей:

```text
domain <- application <- infrastructure/adapters <- bootstrap
```

Модули:

| Модуль | Статус | Ответственность |
| --- | --- | --- |
| `domain` | Реализован | Доменные агрегаты, статусы, политики, правила |
| `application` | Реализован основной слой | Use cases, DTO, mappers, outbound ports |
| `infrastructure` | Реализован основной слой | JPA/Flyway persistence, executors, limit checkers, process runner |
| `adapters` | Реализован web layer | REST controllers, request DTO, validation, exception handling, Swagger |
| `bootstrap` | Реализован | Spring Boot entry point, runtime config |
| `frontend` | Реализован базовый UI | React pages, reusable UI components, mock API, HTTP API boundary |

## 3. Backend: реализовано

Domain:

- `Project` со статусами `ACTIVE`, `DISABLED`, `ARCHIVED`;
- `AiTool` со статусами `ENABLED`, `DISABLED` и типами `FAKE`, `CODEX`,
  `CLAUDE_CODE`, `CUSTOM`;
- `PromptQueue` со статусами `CREATED`, `WAITING_LIMIT`,
  `WAITING_CONFIRMATION`, `RUNNING`, `PAUSED`, `STOPPED`, `COMPLETED`,
  `DISABLED`;
- `Prompt` со статусами `DRAFT`, `QUEUED`, `WAITING_LIMIT`,
  `WAITING_CONFIRMATION`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`,
  `SKIPPED`;
- `PromptExecution` с результатом запуска;
- `QueueExecutionPolicy`, `WorkingHours`, `NextPromptSelector`,
  `PromptOrderingService`.

Application:

- `ProjectApplicationService`;
- `AiToolApplicationService`;
- `PromptQueueApplicationService`;
- `PromptApplicationService`;
- `QueueRunnerApplicationService`;
- repository ports для projects, AI tools, queues, prompts, executions;
- `PromptExecutor` port;
- `AiLimitChecker` port;
- DTO и mapper слои для application boundary.

Infrastructure:

- Flyway migration `V1__initial_schema.sql`;
- JPA entities для projects, AI tools, prompt queues, prompts, executions;
- Spring Data repositories;
- persistence adapters для application repository ports;
- `FakePromptExecutor`;
- `ProcessRunner` на `ProcessBuilder` без shell wrapping;
- `CodexCliPromptExecutor`;
- `CodexCommandBuilder`;
- `CodexOutputParser`;
- `FakeAiLimitChecker`;
- `CodexCliLimitChecker`;
- `CodexLimitCheckCommandBuilder`;
- `CodexLimitOutputParser`;
- Docker-aware integration tests.

Adapters:

- REST controllers:
  - `/api/v1/projects`;
  - `/api/v1/ai-tools`;
  - `/api/v1/queues`;
  - `/api/v1/prompts`;
  - `/api/v1/queues/{queueId}/runner`;
- request validation через Jakarta Validation;
- `GlobalExceptionHandler`;
- `ErrorResponse`;
- Swagger/OpenAPI config.

Bootstrap:

- `AiqApplication`;
- application config;
- Spring Boot packaging;
- context test с Testcontainers PostgreSQL, который автоматически пропускается,
  если Docker недоступен.

Docker/runtime:

- основной режим для реального Codex: PostgreSQL в Docker, backend и Codex CLI
  на хосте;
- demo-режим: `docker compose --profile demo up --build` поднимает PostgreSQL,
  backend и frontend в Docker;
- в demo-режиме backend использует fake executor, Codex executor выключен;
- запуск реального Codex внутри контейнера пока не рекомендуется без отдельного
  host-side runner или явных volume mounts для project directories и Codex auth.

## 4. Frontend: реализовано

Frontend находится в `frontend/`.

Стек:

- React 19;
- TypeScript;
- Vite;
- Tailwind utility classes через CDN fallback;
- mock API by default;
- HTTP API client boundary для будущего подключения к Spring Boot backend.

Страницы:

- Dashboard;
- Projects;
- AI Tools;
- Queues;
- Settings.

Структура:

```text
frontend/
├── src/
│   ├── App.tsx
│   ├── api.ts
│   ├── types.ts
│   ├── components/
│   ├── data/
│   ├── lib/
│   └── pages/
├── package.json
├── vite.config.ts
└── README.md
```

Frontend пока работает как UI shell на mock data. HTTP-режим включается через:

```bash
VITE_USE_MOCK_API=false VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

При включённом VPN лучше использовать `127.0.0.1` вместо `localhost`:

```bash
VITE_USE_MOCK_API=false VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev
```

В HTTP-режиме frontend берёт projects, AI tools, queues и prompts из backend.
Settings и dashboard action `Check Limits` пока работают локально во frontend,
пока для них не добавлены отдельные backend endpoint-ы.

## 5. Limit checker

Реализован v1 без хранения состояния лимита в БД.

Поведение:

- перед запуском prompt `QueueRunnerApplicationService` загружает target AI tool;
- вызывает `AiLimitChecker`;
- если статус `AVAILABLE`, prompt исполняется;
- если статус не `AVAILABLE`, queue переводится в `WAITING_LIMIT`;
- prompt остаётся `QUEUED`;
- `PromptExecution` не создаётся;
- executor не вызывается.

Codex checker использует probe prompt через `codex exec`, потому что отдельной
команды проверки лимитов у Codex CLI нет.

## 6. Тесты и проверки

Проверки, которые сейчас проходят:

```bash
cd backend
mvn clean test
mvn package -DskipTests

cd ../frontend
npm run build
```

Особенность:

- infrastructure persistence tests и bootstrap context test требуют Docker;
- если Docker недоступен, эти тесты пропускаются через `DockerAvailableCondition`;
- на машине с Docker они запускаются как интеграционные PostgreSQL/Testcontainers
  проверки.

## 7. Известные ограничения

- Frontend пока использует mock data и частичный HTTP mapping под текущие summary
  DTO backend.
- В backend пока нет полноценного endpoint для dashboard summary.
- В REST API list queues/list prompts требуют `projectId`/`queueId`, а frontend
  для HTTP-режима пока агрегирует данные через несколько запросов.
- Нет authentication/authorization.
- Нет scheduler для автоматического запуска очередей.
- Нет notifications.
- Нет полноценного UI CRUD: кнопки создания/редактирования пока являются
  заготовками.
- Codex limit checker через probe может потреблять минимальный запрос к Codex.
- Полностью контейнерный режим предназначен для demo/fake executor. Для реального
  Codex backend лучше запускать на хосте, иначе контейнер не увидит локальные
  пути проектов, установленный Codex CLI и пользовательскую авторизацию.

## 8. Что делать дальше

Рекомендуемый порядок:

1. Согласовать frontend DTO с backend response DTO: добавить недостающие поля
   для root directory, counts, timestamps, queue stats, prompt tool name.
2. Добавить backend endpoints для dashboard summary и limit check action.
3. Подключить frontend HTTP mode к реальным endpoint без mock-specific дефолтов.
4. Реализовать формы CRUD в UI: create project, create AI tool, create queue,
   add prompt.
5. Добавить frontend tests для основных страниц и API mapping.
6. Добавить scheduler для auto-run очередей.
7. Добавить notifications.
8. Улучшить executor configuration UI и runtime diagnostics.
9. Спроектировать host-side `agent-runner`, если нужен backend в Docker с
   реальным запуском Codex на хосте.
