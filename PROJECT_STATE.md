# Vibe Code Manager / текущее состояние проекта

Снимок состояния на 2026-06-25.

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
- scheduler автоматического возобновления очередей после восстановления лимита;
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

- `Project` со статусами `ACTIVE`, `DISABLED`, `ARCHIVED` и optional
  `codexSessionId`: один project соответствует одному Codex context/session;
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
- `WaitingLimitQueueScheduler`;
- repository ports для projects, AI tools, queues, prompts, executions;
- `PromptExecutor` port;
- `AiLimitChecker` port;
- DTO и mapper слои для application boundary;
- `PromptDetails.lastExecution` отдаёт последний сохранённый результат запуска:
  parsed response text, raw output, stderr, error message и timing metadata.

Infrastructure:

- Flyway migrations `V1__initial_schema.sql`,
  `V2__codex_project_context.sql`;
- JPA entities для projects, AI tools, prompt queues, prompts, executions;
- Spring Data repositories;
- persistence adapters для application repository ports;
- `FakePromptExecutor`;
- `ProcessRunner` на `ProcessBuilder` без shell wrapping;
- `CodexCliPromptExecutor` запускает Codex с `--sandbox workspace-write`,
  чтобы prompt-ы могли создавать и менять файлы в рабочей директории;
- `CodexCommandBuilder`: первый запуск идёт через `codex exec`, а проекты с
  сохранённой session продолжаются через `codex exec resume <SESSION_ID> -`;
- `CodexOutputParser` извлекает response text и `thread_id` из JSONL;
- сохранение stdout/stderr/raw output Codex и `externalSessionId` в
  `prompt_executions`;
- `FakeAiLimitChecker`;
- `CodexCliLimitChecker`;
- `CodexLimitCheckCommandBuilder`;
- `CodexLimitOutputParser`;
- pessimistic row locking очереди перед ручным или автоматическим запуском;
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

Для projects доступен endpoint `PATCH /api/v1/projects/{projectId}/codex-session`,
который привязывает существующую Codex session или очищает привязку.

Bootstrap:

- `AiqApplication`;
- application config;
- Spring Scheduling;
- Spring Boot packaging;
- context test с Testcontainers PostgreSQL, который автоматически пропускается,
  если Docker недоступен.

Docker/runtime:

- основной режим для реального Codex: PostgreSQL в Docker, backend и Codex CLI
  на хосте, backend стартует с `AIQ_EXECUTOR_CODEX_ENABLED=true`;
- demo-режим: `docker compose --profile demo up --build` поднимает PostgreSQL,
  backend и frontend в Docker;
- в demo-режиме backend использует fake executor, Codex executor выключен;
- если backend запущен без `AIQ_EXECUTOR_CODEX_ENABLED=true`, prompt может
  перейти в `COMPLETED`, но это будет fake execution без изменений файлов;
- запуск реального Codex внутри контейнера пока не рекомендуется без отдельного
  host-side runner или явных volume mounts для project directories и Codex auth.
- рабочая директория prompt-а берётся из `workingDirectoryOverride`, а если он
  пустой — из `rootDirectory` проекта очереди; backend логирует фактический путь
  перед запуском executor-а.
- пути вида `~/project` разворачиваются приложением в домашнюю директорию,
  потому что Codex запускается через `ProcessBuilder` без shell.
- для Codex project хранит `codexSessionId`: если он пустой, первый запуск
  создаёт новую session и сохраняет `thread_id`; если заполнен, следующие
  prompt-ы проекта продолжают эту session через `codex exec resume`.

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

Страница Projects показывает Codex context проекта. При создании проекта можно
оставить `New context on first run` или указать существующий Codex session id;
известные приложению session id доступны как варианты в поле ввода.

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
- если статус `LIMIT_REACHED`, queue переводится в `WAITING_LIMIT`;
- если статус `ERROR` или `UNKNOWN`, queue переводится в `STOPPED` с причиной
  ошибки проверки лимита;
- prompt остаётся `QUEUED`;
- `PromptExecution` не создаётся;
- executor не вызывается.

Очереди `WAITING_LIMIT` автоматически проверяются каждые 60 секунд. При
восстановлении лимита scheduler продолжает очередь независимо от `AutoRunMode`,
так как первоначальный ручной запуск уже считается согласием. За один проход
выполняется не больше `maxPromptsPerRun`; рабочие часы очереди соблюдаются.

Если лимит обнаружен в результате основного `codex exec`, execution сохраняется
как неуспешный для диагностики, prompt возвращается в `QUEUED` без расходования
попытки, а queue переходит в `WAITING_LIMIT`. Pessimistic lock защищает от
двойного запуска одной очереди scheduler-ом и HTTP-запросом.

Codex checker использует probe prompt через `codex exec`, потому что отдельной
команды проверки лимитов у Codex CLI нет. Для Codex probe включён fail-open
режим: техническая ошибка проверки без явного limit-pattern не блокирует основной
executor. Строгий режим включается через `AIQ_LIMIT_CODEX_FAIL_OPEN_ON_ERROR=false`.

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
- Нет notifications.
- Нет полноценного UI CRUD: кнопки создания/редактирования пока являются
  заготовками.
- Codex limit checker через probe может потреблять минимальный запрос к Codex.
- Fake executor помечает prompt как успешно выполненный без запуска реального
  AI CLI. Это ожидаемо для demo/dev режима, но может выглядеть как "prompt
  выполнился, а код не поменялся".
- Полностью контейнерный режим предназначен для demo/fake executor. Для реального
  Codex backend лучше запускать на хосте, иначе контейнер не увидит локальные
  пути проектов, установленный Codex CLI и пользовательскую авторизацию.
- Codex context привязан к project, а не к отдельной queue. Если в одном project
  несколько очередей, они продолжают одну и ту же Codex session.

## 8. Что делать дальше

Рекомендуемый порядок:

1. Согласовать frontend DTO с backend response DTO: добавить недостающие поля
   для counts, timestamps, queue stats, prompt tool name.
2. Добавить backend endpoints для dashboard summary и limit check action.
3. Подключить frontend HTTP mode к реальным endpoint без mock-specific дефолтов.
4. Реализовать формы CRUD в UI: create project, create AI tool, create queue,
   add prompt.
5. Добавить frontend tests для основных страниц и API mapping.
6. Добавить notifications.
7. Улучшить executor configuration UI и runtime diagnostics.
8. Спроектировать host-side `agent-runner`, если нужен backend в Docker с
   реальным запуском Codex на хосте.
