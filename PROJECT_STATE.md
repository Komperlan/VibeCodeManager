# Vibe Code Manager / AI Limit Queue: состояние проекта

Снимок состояния на 2026-06-15.

Этот файл собирает в одном месте текущую информацию по проекту: назначение,
архитектуру, реализованные части, тесты, известные пробелы и ближайший порядок
работ. Источник истины по поведению - код и тесты; `README.md` и `docs/` стоит
синхронизировать с этим файлом по мере развития проекта.

## 1. Коротко

Проект - local-first приложение для управления отложенными очередями промптов
для AI coding tools. Пользователь создаёт проекты, очереди и промпты, а система
запускает промпты, когда это разрешено политикой очереди и доступностью
AI-инструмента.

Текущий backend уже имеет:

- Maven multi-module каркас;
- чистый domain layer с агрегатами, статусами, политиками и unit-тестами;
- application layer с сервисами проектов, очередей, промптов и queue runner;
- outbound ports для репозиториев и executor;
- fake executor в infrastructure;
- Docker Compose для локального PostgreSQL;
- тесты для domain, queue runner и fake executor.

Проект ещё не является функционально готовым приложением: persistence, REST API,
реальные AI CLI executors, уведомления, UI и CLI не реализованы.

## 2. Назначение продукта

Цель проекта - дать пользователю локальный менеджер очередей для AI coding
tools:

- хранить проекты с корневой директорией;
- создавать очереди промптов внутри проекта;
- добавлять промпты для конкретного AI tool;
- запускать промпты по одному или пачкой;
- учитывать приоритет, FIFO-позицию и лимит промптов за запуск;
- сохранять историю исполнения, команду, stdout, stderr, raw output, exit code;
- позже проверять лимиты AI-инструментов и уведомлять пользователя.

Проект не должен обходить rate limits, автоматизировать мультиаккаунты, хранить
пароли или логировать секреты.

## 3. Технологический стек

Текущее:

- Java 21;
- Maven multi-module;
- Spring Boot 4.0.6;
- Spring `@Service` и `@Transactional` в application layer;
- Lombok для снижения boilerplate;
- JUnit 5;
- AssertJ;
- Mockito;
- Spring Data JPA/Flyway/PostgreSQL зависимости подключены в infrastructure;
- Spring Web и Actuator подключены в bootstrap;
- Docker Compose с PostgreSQL 16.

Запланировано:

- REST API;
- PostgreSQL persistence adapters;
- Flyway migrations;
- real process runner на `ProcessBuilder`;
- Codex CLI executor;
- Claude Code executor;
- limit checker;
- notification adapters;
- Picocli CLI;
- React UI.

## 4. Архитектура

Проект строится как модульный монолит в стиле DDD + Hexagonal Architecture /
Ports and Adapters.

Направление зависимостей:

```text
domain <- application <- infrastructure/adapters <- bootstrap
```

Правила слоёв:

- `domain` не знает про Spring, JPA, REST, ProcessBuilder и внешние сервисы;
- `application` координирует use cases и зависит от domain;
- `application` объявляет outbound ports;
- `infrastructure` реализует технические адаптеры;
- `adapters` зарезервирован под будущие AI CLI / notification adapters;
- `bootstrap` собирает runtime-приложение.

JPA entities не должны становиться domain entities. Domain entities остаются
чистыми агрегатами, а persistence должен маппить их отдельно.

## 5. Структура репозитория

```text
.
├── README.md
├── PROJECT_STATE.md
├── docs/
│   └── executors.md
├── docker-compose.yml
└── backend/
    ├── pom.xml
    ├── domain/
    ├── application/
    ├── infrastructure/
    ├── adapters/
    └── bootstrap/
```

Модули backend:

| Модуль | Статус | Ответственность |
| --- | --- | --- |
| `domain` | Реализован базовый core | Агрегаты, value objects, статусы, доменные правила |
| `application` | В работе | Use cases, DTO, mappers, outbound ports, queue runner |
| `infrastructure` | В работе | Fake executor, будущие JPA/Flyway/process adapters |
| `adapters` | Пустой aggregator | Будущие внешние AI/notification adapters |
| `bootstrap` | Каркас | Spring Boot entry point, Web/Actuator зависимости |

## 6. Domain layer

Пакет: `backend/domain/src/main/java/com/aiq/domain`.

### 6.1 Common

`AggregateRoot`

- хранит domain events;
- позволяет добавлять, читать и очищать события.

`DomainEvent`

- базовый interface с `occurredAt()`.

`DomainException`

- базовое runtime-исключение domain layer.

### 6.2 Project

`Project`

Поля:

- `UUID id`;
- `String name`;
- `String rootDirectory`;
- `ProjectStatus status`;
- `Instant createdAt`;
- `Instant updatedAt`.

Статусы:

- `ACTIVE`;
- `DISABLED`;
- `ARCHIVED`.

Методы:

- `create(name, rootDirectory)`;
- `restore(...)`;
- `rename(newName)`;
- `changeRootDirectory(newRootDirectory)`;
- `disable()`;
- `activate()`;
- `archive()`;
- `isActive()`.

Правила:

- name не null, не blank, максимум 100 символов;
- rootDirectory не null и не blank;
- archived project нельзя менять через rename/changeRootDirectory/disable/activate;
- updatedAt не может быть раньше createdAt.

Текущий компромисс: `rootDirectory` валидируется как непустая строка, без
проверки существования директории на файловой системе. Это правильно для domain:
проверка реального FS должна жить в application/infrastructure.

### 6.3 Queue

`PromptQueue`

Поля:

- `UUID id`;
- `UUID projectId`;
- `String name`;
- `QueueStatus status`;
- `QueueExecutionPolicy executionPolicy`;
- `Instant createdAt`;
- `Instant updatedAt`.

Статусы очереди:

- `CREATED`;
- `WAITING_LIMIT`;
- `WAITING_CONFIRMATION`;
- `RUNNING`;
- `PAUSED`;
- `STOPPED`;
- `COMPLETED`;
- `DISABLED`.

Методы:

- `create(projectId, name, policy)`;
- `restore(...)`;
- `start()`;
- `pause()`;
- `resume()`;
- `stop(reason)`;
- `markWaitingLimit()`;
- `markWaitingConfirmation()`;
- `complete()`;
- `disable()`;
- `enable()`;
- `changeExecutionPolicy(policy)`;
- `canRun()`;
- `shouldStopOnError()`.

Правила:

- running queue нельзя стартовать повторно;
- paused queue надо resume, а не start;
- disabled/completed queue нельзя start;
- pause только из RUNNING;
- resume только из PAUSED или STOPPED;
- completed/disabled queue нельзя stop;
- disabled queue нельзя complete;
- enable только из DISABLED.

`QueueExecutionPolicy`

Поля:

- `AutoRunMode autoRunMode`;
- `int maxPromptsPerRun`;
- `Duration cooldown`;
- `boolean stopOnError`;
- `boolean workingHoursEnabled`;
- `WorkingHours workingHours`.

Default policy:

- `ASK_CONFIRMATION`;
- `maxPromptsPerRun = 3`;
- `cooldown = 60s`;
- `stopOnError = true`;
- working hours выключены.

`AutoRunMode`

- `NOTIFY_ONLY`;
- `ASK_CONFIRMATION`;
- `AUTO_RUN`.

`Prompt`

Поля:

- `UUID id`;
- `UUID queueId`;
- `UUID targetAiToolId`;
- `String title`;
- `String content`;
- `int priority`;
- `long position`;
- `PromptStatus status`;
- optional `workingDirectoryOverride`;
- `int attemptCount`;
- `int maxAttempts`;
- timestamps;
- optional `failureReason`.

Статусы prompt:

- `DRAFT`;
- `QUEUED`;
- `WAITING_LIMIT`;
- `WAITING_CONFIRMATION`;
- `RUNNING`;
- `COMPLETED`;
- `FAILED`;
- `CANCELLED`;
- `SKIPPED`.

Методы:

- `createQueued(...)`;
- `createDraft(...)`;
- `restore(...)`;
- `enqueue()`;
- `markWaitingLimit()`;
- `markWaitingConfirmation()`;
- `start()`;
- `complete()`;
- `fail(reason)`;
- `retry()`;
- `cancel()`;
- `skip()`;
- `changePriority(priority)`;
- `changeContent(content)`;
- `changeTitle(title)`;
- `canRetry()`;
- `isQueued()`;
- `isTerminal()`.

Правила:

- title не null, не blank, максимум 150 символов;
- content не null, не blank, максимум 50 000 символов;
- priority не может быть отрицательным;
- position не может быть отрицательным;
- maxAttempts должен быть положительным;
- attemptCount не может быть отрицательным и не может превышать maxAttempts;
- start увеличивает attemptCount;
- retry переводит FAILED обратно в QUEUED, если остались попытки.

`position` - это постоянная FIFO-позиция prompt в очереди. Она не
перенумеровывается после выполнения: если 10 промптов уже исполнены, следующий
новый prompt получает позицию 11.

`PromptOrderingService`

- сортирует prompt-ы: priority по убыванию, затем position по возрастанию,
  затем createdAt по возрастанию.

`NextPromptSelector`

- выбирает только prompt-ы со статусом `QUEUED`;
- применяет порядок из `PromptOrderingService`.

### 6.4 Safety

`WorkingHours`

- хранит рабочий интервал;
- поддерживает интервалы через полночь;
- используется в `QueueExecutionPolicy`, но application runner пока не
применяет это правило.

### 6.5 Execution

`PromptExecution`

Поля:

- `UUID id`;
- `UUID promptId`;
- `UUID aiToolId`;
- `ExecutionStatus status`;
- `String command`;
- optional `ExecutionResult result`;
- optional `startedAt`;
- optional `finishedAt`;
- optional `duration`.

Статусы execution:

- `CREATED`;
- `RUNNING`;
- `COMPLETED`;
- `FAILED`;
- `CANCELLED`;
- `TIMEOUT`.

Методы:

- `create(promptId, aiToolId, command)`;
- `restore(...)`;
- `start()`;
- `complete(executionResult)`;
- `cancel()`;
- `timeout()`;
- `isFinished()`.

`ExecutionResult`

- `int exitCode`;
- `String stdout`;
- `String stderr`;
- `String rawOutput`;
- `String errorMessage`;
- `isSuccessful()` возвращает true при `exitCode == 0`.

## 7. Application layer

Пакет: `backend/application/src/main/java/com/aiq/application`.

Application layer использует DTO и mapper-ы, чтобы не отдавать domain entities
наружу. Это оставляет domain модель внутренней и даёт свободу менять API без
поломки доменных классов.

### 7.1 Outbound ports

`ProjectRepository`

- `save(Project project)`;
- `findById(UUID projectId)`;
- `findAll()`;
- `existsByRootDirectory(String rootDirectory)`.

`PromptQueueRepository`

- `save(PromptQueue queue)`;
- `findById(UUID queueId)`;
- `findByProjectId(UUID projectId)`;
- `findByStatuses(Collection<QueueStatus> statuses)`.

`PromptRepository`

- `save(Prompt prompt)`;
- `findById(UUID promptId)`;
- `findByQueueId(UUID queueId)`;
- `findByQueueIdAndStatus(UUID queueId, PromptStatus status)`;
- `nextPosition(UUID queueId)`.

`PromptExecutionRepository`

- `save(PromptExecution execution)`;
- `findById(UUID executionId)`;
- `findByPromptId(UUID promptId)`;
- `findLatestByPromptId(UUID promptId)`.

`AiToolRepository`

- `existsById(UUID aiToolId)`.

Важно: port уже есть, но полноценной domain/application модели `AiTool` пока
нет. Сейчас prompt service умеет только проверять существование tool id.

`PromptExecutor`

- `buildCommand(PromptExecutionRequest request)`;
- `execute(PromptExecutionRequest request)`.

### 7.2 ProjectApplicationService

Методы:

- `createProject(command)` - проверяет уникальность rootDirectory, создаёт
  Project, сохраняет, возвращает `CreateProjectResult`;
- `getProject(projectId)` - возвращает `ProjectDetails`;
- `listProjects()` - возвращает список `ProjectSummary`;
- `renameProject(projectId, name)` - меняет имя;
- `changeRootDirectory(projectId, rootDirectory)` - меняет root directory;
- `disableProject(projectId)` - переводит проект в DISABLED;
- `activateProject(projectId)` - переводит проект в ACTIVE;
- `archiveProject(projectId)` - переводит проект в ARCHIVED.

### 7.3 PromptQueueApplicationService

Методы:

- `createQueue(command)` - проверяет существование project, создаёт queue,
  сохраняет, возвращает `CreateQueueResult`;
- `getQueue(queueId)` - возвращает `QueueDetails`;
- `listProjectQueues(projectId)` - возвращает очереди проекта;
- `startQueue(queueId)`;
- `pauseQueue(queueId)`;
- `resumeQueue(queueId)`;
- `stopQueue(queueId, reason)`;
- `disableQueue(queueId)`;
- `enableQueue(queueId)`;
- `changeExecutionPolicy(queueId, command)`.

Замечания:

- в `createQueue` текст null-check сейчас говорит `Create project command must
  not be null`, лучше заменить на queue command;
- `listProjectQueues` сейчас не проверяет существование project;
- `stopQueue` требует non-null reason, хотя domain пока reason не хранит.

### 7.4 PromptApplicationService

Методы:

- `addPrompt(command)` - проверяет queue и aiTool, берёт nextPosition, создаёт
  QUEUED prompt, сохраняет;
- `addDraftPrompt(command)` - то же самое, но создаёт DRAFT prompt;
- `getPrompt(promptId)` - возвращает `PromptDetails`;
- `listQueuePrompts(queueId)` - проверяет queue, возвращает отсортированные
  `PromptSummary`;
- `enqueuePrompt(promptId)` - переводит DRAFT в QUEUED;
- `changePromptTitle(promptId, title)`;
- `changePromptContent(promptId, content)`;
- `changePromptPriority(promptId, priority)`;
- `cancelPrompt(promptId)`;
- `skipPrompt(promptId)`.

### 7.5 QueueRunnerApplicationService

Методы:

- `runQueue(command)` - запускает очередь пачкой до command limit и policy
  limit;
- `runNextPrompt(queueId)` - запускает один следующий prompt.

Текущее поведение `runNextPrompt`:

1. Загружает queue.
2. Проверяет, что queue можно запускать.
3. Загружает prompts по queue id.
4. Выбирает следующий QUEUED prompt через `NextPromptSelector`.
5. Если QUEUED prompts нет, завершает queue.
6. Стартует queue, если она ещё не RUNNING.
7. Создаёт `PromptExecution`.
8. Переводит prompt и execution в RUNNING.
9. Вызывает `PromptExecutor`.
10. Завершает execution.
11. При успехе завершает prompt.
12. При ошибке:
    - prompt получает FAILED;
    - если `stopOnError = true`, queue получает STOPPED;
    - если `stopOnError = false` и остались попытки, prompt возвращается в
      QUEUED.
13. Сохраняет execution и prompt.
14. Если после успешного prompt больше нет QUEUED prompts, завершает queue.

Текущее поведение `runQueue`:

- валидирует command и положительный maxPrompts;
- берёт минимум из command limit и `queue.executionPolicy.maxPromptsPerRun`;
- последовательно вызывает `runNextPrompt`;
- останавливается на отсутствии prompt, completion, stopped queue или лимите;
- если лимит достигнут и prompts ещё есть, переводит queue в STOPPED с причиной
  `Run limit reached`.

Пока не реализовано в runner:

- проверка AI limit перед запуском;
- `WAITING_LIMIT`;
- `WAITING_CONFIRMATION`;
- `AutoRunMode`;
- cooldown между prompt-ами;
- working hours;
- concurrency lock;
- recovery после рестарта приложения;
- scheduler;
- сохранение/вывод reason у queue;
- timeout как отдельный status execution.

## 8. Infrastructure layer

Пакет: `backend/infrastructure/src/main/java/com/aiq/infrastructure`.

### 8.1 FakePromptExecutor

Файл: `executor/FakePromptExecutor.java`.

Статус: реализован.

Поведение:

- Spring bean включён по умолчанию;
- отключается property `aiq.executor.fake.enabled=false`;
- `buildCommand` возвращает строку вида
  `fake-executor --ai-tool-id <id> --prompt-id <id>`;
- если есть `workingDirectoryOverride`, добавляет `--workdir`;
- `execute` возвращает success result с `exitCode = 0`;
- если content содержит `[fake-fail]`, возвращает failed result с
  `exitCode = 1`.

Назначение:

- тестировать queue runner;
- запускать будущие REST/UI сценарии без Codex CLI и Claude Code;
- иметь быстрый dev executor.

### 8.2 Persistence

Подключены зависимости:

- Spring Data JPA;
- Flyway;
- PostgreSQL runtime driver;
- Testcontainers PostgreSQL для будущих integration tests.

Не реализовано:

- JPA entities;
- Spring Data repositories;
- adapters, реализующие application ports;
- Flyway migrations;
- mapper-ы domain <-> persistence model;
- integration tests persistence layer.

## 9. Bootstrap

Пакет: `backend/bootstrap`.

Реализовано:

- `AiqApplication`;
- Spring Boot plugin с main class;
- зависимости на domain/application/infrastructure;
- Spring Web;
- Spring Boot Actuator.

Пока пусто/не реализовано:

- `application.yml` не содержит runtime-настроек;
- datasource config;
- JPA/Flyway config;
- REST controllers;
- exception handlers;
- OpenAPI;
- security/CORS;
- scheduler enablement.

## 10. Adapters module

`backend/adapters` сейчас является пустым Maven aggregator module.

Пока нет:

- Codex CLI adapter;
- Claude Code adapter;
- custom command adapter;
- Telegram notification adapter;
- desktop notification adapter.

## 11. Executor plan

Подробности лежат в `docs/executors.md`.

Ключевые правила для реальных executors:

- executor запускает ровно один prompt;
- executor не выбирает следующий prompt;
- executor не меняет статусы domain entities;
- executor не делает retry;
- executor не проверяет лимиты;
- реальные CLI надо запускать через `ProcessBuilder` списком аргументов;
- не использовать shell string (`sh -c`, `bash -c`) для обычного запуска;
- secrets нельзя писать в command, логи, stdout/stderr sanitization;
- timeout и output limit должны быть частью общего process runner.

Перед реальными Codex/Claude executors нужно сделать:

- `ProcessCommand`;
- `ProcessRunResult`;
- `ProcessRunner`;
- command builders;
- output parsers;
- properties через Spring `@ConfigurationProperties`;
- fixture tests для parser-ов;
- unit tests process runner.

Для Codex CLI и Claude Code нельзя зашивать флаги по памяти: перед
реализацией надо проверить `codex --help`, `claude --help` или официальную
документацию.

## 12. Docker Compose

`docker-compose.yml` содержит PostgreSQL:

- image `postgres:16-alpine`;
- container `aiqueue-postgres`;
- database `aiqueue`;
- user `aiqueue`;
- password `aiqueue`;
- port `5432:5432`;
- volume `postgres_data`;
- healthcheck через `pg_isready`.

Команды:

```bash
docker compose up -d postgres
docker compose down
```

## 13. Тесты

Текущий тестовый слой содержит 102 `@Test`:

- domain: 51;
- application: 48;
- infrastructure: 3.

Проверено локально: `cd backend && mvn clean verify` - `BUILD SUCCESS`.

Покрыто:

- создание/валидация Project;
- WorkingHours, включая интервалы через полночь;
- QueueExecutionPolicy;
- PromptQueue transitions;
- Prompt lifecycle;
- ordering и next prompt selection;
- PromptExecution lifecycle;
- QueueRunnerApplicationService:
  - successful execution;
  - priority ordering;
  - FIFO position при равном priority;
  - игнорирование non-QUEUED prompts;
  - empty queue completion;
  - paused queue не запускается;
  - failure + stopOnError;
  - retry при stopOnError=false;
  - executor exception превращается в failed ExecutionResult;
  - stderr/errorMessage/exitCode fallback для failure reason;
  - command max limit;
  - policy max limit;
  - null/missing/invalid inputs;
- FakePromptExecutor:
  - build command;
  - success;
  - fake failure marker.

Команды:

```bash
cd backend && mvn clean verify
cd backend && mvn -pl domain test
cd backend && mvn -pl application test
cd backend && mvn -pl infrastructure test
```

## 14. Текущее состояние по крупным фичам

| Фича | Статус | Комментарий |
| --- | --- | --- |
| Maven multi-module backend | Готово | 5 модулей |
| Domain Queue Core | Готово для MVP | Есть тесты |
| Application services | В работе | Основные сервисы есть, не все правила runner реализованы |
| DTO и mappers | В работе | Есть для project/prompt/queue/runner |
| QueueRunner | В работе | Есть sync запуск через PromptExecutor |
| Fake executor | Готово для dev/test | Реализован в infrastructure |
| PostgreSQL persistence | Не начато | Только зависимости и Docker |
| Flyway migrations | Не начато | Миграций нет |
| REST API | Не начато | Web dependency есть, controllers нет |
| AI Tool model/service | Не начато | Есть только `AiToolRepository.existsById` |
| Limit checker | Не начато | Нужен отдельный port |
| Scheduler | Не начато | Нужен после persistence/limit checker |
| Codex CLI executor | Не начато | Нужен ProcessRunner |
| Claude Code executor | Не начато | Нужен ProcessRunner |
| Notifications | Не начато | Telegram/Desktop adapters отсутствуют |
| React UI | Не начато | Frontend отсутствует |
| Picocli CLI | Не начато | CLI module отсутствует |

## 15. Известные технические долги и риски

1. `application.yml` пустой, runtime config пока не оформлен.
2. Нет persistence adapters, поэтому application services пока нельзя
   использовать в реальном приложении.
3. Нет `AiTool` domain/application модели, но prompt service уже требует
   `AiToolRepository`.
4. Queue runner пока не применяет `AutoRunMode`, cooldown и working hours.
5. Queue runner пока не проверяет лимиты AI tools.
6. Нет concurrency guard: два параллельных запуска одной queue могут конфликтовать.
7. Нет отдельной модели причины остановки queue, хотя сервисы передают reason.
8. `PromptQueueApplicationService.createQueue` имеет неточный текст null-check.
9. `PromptQueueApplicationService.listProjectQueues` не проверяет project id через
   `ProjectRepository`.
10. Нет единых application-level exceptions, сейчас используются
    `IllegalArgumentException` / `NullPointerException`.
11. Нет REST error model.
12. Нет migrations и схемы БД, поэтому репозитории ещё не зафиксированы.

## 16. Рекомендуемый порядок следующих работ

### Шаг 1. Подчистить application services

- поправить message в `createQueue`;
- решить, надо ли `listProjectQueues` проверять существование project;
- решить, должен ли `stopQueue` принимать nullable reason;
- ввести application exception types:
  - `NotFoundException`;
  - `ValidationException`;
  - возможно `ConflictException`.

### Шаг 2. Добавить AI Tool минимальную модель

Нужно, потому что prompt уже ссылается на `targetAiToolId`.

Минимально:

- domain entity/value model `AiTool`;
- статусы enabled/disabled;
- type: `FAKE`, `CODEX`, `CLAUDE_CODE`, `CUSTOM`;
- application service:
  - create tool;
  - get/list tools;
  - enable/disable;
  - update executable/config;
- repository port вместо одного `existsById` или расширение текущего port.

### Шаг 3. Реализовать persistence MVP

Сначала JPA/Flyway для:

- projects;
- prompt queues;
- prompts;
- prompt executions;
- ai tools.

Нужно сделать:

- migrations `V1__...sql`;
- JPA entities отдельно от domain;
- Spring Data repositories;
- adapters для application ports;
- mapper-ы persistence <-> domain;
- Testcontainers integration tests.

### Шаг 4. Подключить bootstrap config

- заполнить `application.yml`;
- datasource;
- Flyway;
- JPA settings;
- fake executor property;
- actuator endpoints;
- профили `local`/`test`.

### Шаг 5. REST API MVP

Минимальные endpoints:

- projects CRUD-like commands;
- queues commands;
- prompts commands;
- run queue / run next prompt;
- executions read endpoints;
- ai tools CRUD-like commands.

Нужно:

- controllers;
- request/response DTO;
- exception handler;
- validation;
- basic integration tests.

### Шаг 6. Limit checker и runner policy

Добавить port:

- `AiLimitChecker.checkLimit(...)`.

Результаты:

- `AVAILABLE`;
- `LIMIT_REACHED`;
- `UNKNOWN`;
- optional `resetAt`.

После этого runner должен:

- проверять лимит перед запуском;
- переводить queue/prompt в `WAITING_LIMIT`;
- учитывать `AutoRunMode`;
- учитывать working hours;
- учитывать cooldown.

### Шаг 7. ProcessRunner

Реализовать безопасную общую инфраструктуру запуска CLI:

- args list, не shell string;
- working directory;
- env;
- timeout;
- max output bytes;
- параллельное чтение stdout/stderr;
- безопасное логирование.

### Шаг 8. Codex/Claude executors

После ProcessRunner:

- проверить актуальные CLI-флаги;
- сделать command builders;
- сделать output parsers;
- добавить properties;
- покрыть parser fixtures и executor unit tests.

## 17. Практические команды

Сборка всего backend:

```bash
cd backend && mvn clean verify
```

Запуск PostgreSQL:

```bash
docker compose up -d postgres
```

Остановка PostgreSQL:

```bash
docker compose down
```

Поиск файлов:

```bash
rg --files
```

Поиск тестов:

```bash
rg -n "@Test" backend
```

## 18. Главный вывод

Проект сейчас находится в хорошей точке для перехода от чистой модели к
рабочему приложению. Domain layer уже достаточно устойчивый, queue runner
получил тестовое покрытие, fake executor позволяет двигаться без реальных AI
CLI.

Самый полезный следующий фокус - persistence + минимальная AI Tool модель.
Без них REST API и реальные сценарии быстро начнут упираться в отсутствие
хранилища и конфигурации инструментов.
