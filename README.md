# Vibe Code Manager

Local-first приложение для управления проектами, AI-инструментами, очередями
промптов и запуском этих очередей через executor adapters.

Подробная техническая информация вынесена в
[`docs/technical-overview.md`](docs/technical-overview.md). Детали по fake,
Codex CLI и будущим executor adapters лежат в
[`docs/executors.md`](docs/executors.md).

## Что уже есть

- Maven multi-module backend: `domain`, `application`, `infrastructure`,
  `adapters`, `bootstrap`.
- REST API для projects, AI tools, queues, prompts и queue runner.
- PostgreSQL/Flyway persistence.
- Fake executor включён по умолчанию.
- Codex CLI executor реализован, но выключен по умолчанию.
- Project хранит один Codex context/session и продолжает его через
  `codex exec resume`.
- Codex limit checker реализован через безопасный probe-запуск.
- Очереди автоматически возобновляются после восстановления лимита Codex.
- React + TypeScript frontend по Figma Make макету с mock data.
- Swagger UI доступен после запуска приложения.

## Требования

- Java 21
- Maven 3.9+
- Docker и Docker Compose
- Node.js 20+ и npm для frontend

## Быстрый запуск

### Рекомендуемый режим с реальным Codex

```bash
docker compose up -d postgres
```

Backend запускается на хосте, чтобы он видел локальные пути проектов,
установленный `codex`, auth/config и привычное dev-окружение:

```bash
mvn -f backend/pom.xml -pl bootstrap -am -DskipTests package

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aiqueue \
SPRING_DATASOURCE_USERNAME=aiqueue \
SPRING_DATASOURCE_PASSWORD=aiqueue \
AIQ_EXECUTOR_CODEX_ENABLED=true \
AIQ_EXECUTOR_CODEX_EXECUTABLE_PATH=codex \
java -jar backend/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar
```

Важно: без `AIQ_EXECUTOR_CODEX_ENABLED=true` backend использует fake executor.
В этом режиме prompt будет помечен как `COMPLETED`, но Codex не запустится и
файлы проекта не изменятся.

Frontend можно запускать локально:

```bash
cd frontend
npm install
VITE_USE_MOCK_API=false VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev
```

После старта:

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: `localhost:5432`, database/user/password: `aiqueue`

### Demo-режим полностью в Docker

Для просмотра UI/API без реального агента можно поднять backend и frontend в
Docker. В этом режиме включён fake executor, а Codex executor выключен:

```bash
docker compose --profile demo up --build
```

Остановить:

```bash
docker compose down
```

Удалить также данные PostgreSQL:

```bash
docker compose down -v
```

Codex executor внутри контейнера не включается по умолчанию. Для него нужна
отдельная архитектура: host-side runner или явные mounts для `codex`, auth/config
и локальных project directories.

### Backend

Запустить PostgreSQL:

```bash
docker compose up -d postgres
```

Запустить backend на хосте:

```bash
mvn -f backend/pom.xml -pl bootstrap -am -DskipTests package

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aiqueue \
SPRING_DATASOURCE_USERNAME=aiqueue \
SPRING_DATASOURCE_PASSWORD=aiqueue \
java -jar backend/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar
```

Эта команда запускает backend в dev/demo-режиме с fake executor. Для реального
Codex запускай так:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/aiqueue \
SPRING_DATASOURCE_USERNAME=aiqueue \
SPRING_DATASOURCE_PASSWORD=aiqueue \
AIQ_EXECUTOR_CODEX_ENABLED=true \
AIQ_EXECUTOR_CODEX_EXECUTABLE_PATH=codex \
java -jar backend/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar
```

Проверь, что команда доступна из того же терминала:

```bash
which codex
codex --version
```

После старта:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Frontend

Запустить UI на mock data:

```bash
cd frontend
npm install
npm run dev
```

Открыть: http://localhost:5173

Подключить UI к Spring Boot backend:

```bash
VITE_USE_MOCK_API=false VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev
```

В HTTP-режиме основные списки берутся из backend. Settings и кнопка `Check
Limits` пока остаются локальными frontend-заглушками, потому что отдельных
backend endpoint-ов для них ещё нет.

Если включён VPN и UI показывает `NetworkError when attempting to fetch resource`,
используй `127.0.0.1` вместо `localhost` и открывай UI по адресу
http://127.0.0.1:5173. Некоторые VPN/proxy-клиенты перехватывают `localhost` или
меняют DNS/IPv6-поведение, а loopback IP обычно обходит эту проблему.

Остановить PostgreSQL:

```bash
docker compose down
```

## Проверка проекта

Запустить все тесты:

```bash
mvn -f backend/pom.xml test
cd frontend && npm run build
```

Infrastructure и bootstrap tests используют Testcontainers. Если Docker
недоступен, Docker-зависимые PostgreSQL/context tests будут пропущены; на машине
с Docker они запускаются как интеграционные проверки.

Проверить только отдельный модуль:

```bash
mvn -f backend/pom.xml -pl domain test
mvn -f backend/pom.xml -pl application test
mvn -f backend/pom.xml -pl infrastructure -am test
mvn -f backend/pom.xml -pl adapters -am test
```

## Настройка executor

По умолчанию используется fake executor:

```yaml
aiq:
  executor:
    fake:
      enabled: true
    codex:
      enabled: false
```

Включить Codex CLI executor можно через properties/env:

```bash
AIQ_EXECUTOR_CODEX_ENABLED=true \
AIQ_EXECUTOR_CODEX_EXECUTABLE_PATH=codex \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aiqueue \
SPRING_DATASOURCE_USERNAME=aiqueue \
SPRING_DATASOURCE_PASSWORD=aiqueue \
java -jar backend/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar
```

Codex executor запускает
`codex exec --json --color never --sandbox workspace-write --skip-git-repo-check -`
и передаёт prompt через stdin.

Каждый project может быть привязан к одному Codex context/session. Если
`codexSessionId` у проекта пустой, первый успешный Codex-запуск создаёт session,
backend извлекает `thread_id` из JSONL-вывода и сохраняет его в проект. Если
session уже указана, следующие prompt-ы проекта запускаются через
`codex exec resume <SESSION_ID> -`, то есть продолжают тот же Codex-контекст.
Существующую session можно указать при создании проекта или поменять позже через
UI/endpoint `PATCH /api/v1/projects/{projectId}/codex-session`.

Флаг `--sandbox workspace-write` обязателен для сценариев, где Codex должен
создавать или менять файлы. Без него Codex может ответить, что workspace
read-only, и prompt завершится текстовым сообщением без изменений в проекте.

Рабочая директория для запуска выбирается так:

1. если у prompt заполнен `workingDirectoryOverride`, используется он;
2. иначе используется `rootDirectory` проекта, к которому привязана очередь.

Пути вида `~/project` поддерживаются и разворачиваются в домашнюю директорию
пользователя перед запуском CLI. Это сделано в приложении явно, потому что
`ProcessBuilder` запускает Codex без shell и сам не умеет раскрывать `~`.

Backend пишет в лог строку `Preparing prompt ... in working directory ...`, по ней
можно проверить, куда реально отправлен Codex.

Текст, который вернул Codex, сохраняется в `prompt_executions.result_stdout`.
Сырые stdout/stderr/raw output и `externalSessionId` тоже сохраняются в
`prompt_executions`. В API последний результат доступен через
`GET /api/v1/prompts/{promptId}` в поле `lastExecution.responseText`; во frontend
его можно открыть кликом по prompt-у на странице Queues.

Важно: `Project = один Codex-контекст`, но это не бесконечная память. Codex всё
равно ограничен своим context window и может компактировать старую историю.

AI tool в UI с `type = CODEX` и `executablePath = codex` выбирает целевой tool
для prompt-а, но сам по себе не переключает backend executor. Переключение
runtime-исполнителя делается только через env/property при старте backend:
`AIQ_EXECUTOR_CODEX_ENABLED=true`.

Перед выполнением prompt runner вызывает limit checker. Если checker возвращает
`LIMIT_REACHED`, очередь переводится в `WAITING_LIMIT`, prompt остаётся
`QUEUED`, execution не создаётся. Если checker падает или возвращает
`ERROR/UNKNOWN`, очередь переводится в `STOPPED` с причиной ошибки проверки,
чтобы техническая проблема не выглядела как превышенный лимит.

При включённом Codex executor backend раз в 60 секунд проверяет сохранённые
очереди `WAITING_LIMIT`. Когда лимит снова доступен, очередь автоматически
продолжает работу с учётом `maxPromptsPerRun` и настроенных рабочих часов. Это
работает и после перезапуска backend, потому что статус очереди хранится в БД.

Если сообщение о лимите вернул уже основной `codex exec`, неудачный execution
сохраняется для диагностики, но prompt возвращается в `QUEUED` без расходования
retry. Повторный запуск также выполнит scheduler.

Настройки автоматического возобновления:

```yaml
aiq:
  queue:
    limit-resume:
      enabled: true
      poll-interval: 60s
```

Отключить scheduler можно через `AIQ_QUEUE_LIMIT_RESUME_ENABLED=false`, а
изменить интервал — через `AIQ_QUEUE_LIMIT_RESUME_POLL_INTERVAL=5m`. Scheduler
не запускается в demo-режиме с fake executor.

Для Codex checker-а включён fail-open режим: если probe-запуск сломался по
технической причине, но в выводе нет явного `rate limit`, `quota`, `429` или
похожего сообщения, основной prompt всё равно запускается. Отключить это можно:

```bash
AIQ_LIMIT_CODEX_FAIL_OPEN_ON_ERROR=false
```

## Как пользоваться API

Ниже минимальный happy path. Удобнее всего выполнять его через Swagger UI.

### 1. Создать проект

```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Vibe Code Manager",
    "rootDirectory": "/home/user/projects/vibe-code-manager",
    "codexSessionId": null
  }'
```

Если хочешь продолжить уже существующую Codex session, вместо `null` укажи её
id. Если оставить `null`, backend сохранит новую session после первого запуска
Codex prompt-а.

### 2. Зарегистрировать AI tool

```bash
curl -X POST http://localhost:8080/api/v1/ai-tools \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Fake executor",
    "type": "FAKE",
    "executablePath": "fake-executor"
  }'
```

Для Codex CLI используйте:

```json
{
  "name": "Codex CLI",
  "type": "CODEX",
  "executablePath": "codex"
}
```

### 3. Создать очередь

```bash
curl -X POST http://localhost:8080/api/v1/queues \
  -H 'Content-Type: application/json' \
  -d '{
    "projectId": "<PROJECT_ID>",
    "name": "Main queue",
    "executionPolicy": {
      "autoRunMode": "NOTIFY_ONLY",
      "maxPromptsPerRun": 3,
      "cooldown": "PT0S",
      "stopOnError": true,
      "workingHoursEnabled": false,
      "workingHours": null
    }
  }'
```

### 4. Добавить prompt

```bash
curl -X POST http://localhost:8080/api/v1/prompts \
  -H 'Content-Type: application/json' \
  -d '{
    "queueId": "<QUEUE_ID>",
    "targetAiToolId": "<AI_TOOL_ID>",
    "title": "Fix tests",
    "content": "Run Maven tests and fix failures.",
    "priority": 10,
    "maxAttempts": 3,
    "workingDirectoryOverride": null
  }'
```

`workingDirectoryOverride` опционален. Если оставить `null`, prompt будет
выполнен в `rootDirectory` проекта очереди.

### 5. Запустить очередь

```bash
curl -X POST http://localhost:8080/api/v1/queues/<QUEUE_ID>/runner/run \
  -H 'Content-Type: application/json' \
  -d '{"maxPrompts": 3}'
```

Запустить только следующий prompt:

```bash
curl -X POST http://localhost:8080/api/v1/queues/<QUEUE_ID>/runner/run-next
```

## Основные endpoints

```text
POST /api/v1/projects
GET  /api/v1/projects
GET  /api/v1/projects/{projectId}
PATCH /api/v1/projects/{projectId}/codex-session

POST /api/v1/ai-tools
GET  /api/v1/ai-tools
GET  /api/v1/ai-tools/{aiToolId}

POST /api/v1/queues
GET  /api/v1/queues
GET  /api/v1/queues/{queueId}

POST /api/v1/prompts
POST /api/v1/prompts/drafts
GET  /api/v1/prompts
GET  /api/v1/prompts/{promptId}

POST /api/v1/queues/{queueId}/runner/run
POST /api/v1/queues/{queueId}/runner/run-next
```

Полный список endpoints и DTO смотрите в Swagger UI.
