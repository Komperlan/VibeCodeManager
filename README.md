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
- Codex limit checker реализован через безопасный probe-запуск.
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
java -jar backend/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar
```

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
AIQ_EXECUTOR_FAKE_ENABLED=false \
AIQ_EXECUTOR_CODEX_ENABLED=true \
AIQ_EXECUTOR_CODEX_EXECUTABLE_PATH=codex \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aiqueue \
SPRING_DATASOURCE_USERNAME=aiqueue \
SPRING_DATASOURCE_PASSWORD=aiqueue \
java -jar backend/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar
```

Codex executor запускает `codex exec --json --color never --skip-git-repo-check -`
и передаёт prompt через stdin.

Перед выполнением prompt runner вызывает limit checker. Если лимит недоступен,
очередь переводится в `WAITING_LIMIT`, prompt остаётся `QUEUED`, execution не
создаётся.

## Как пользоваться API

Ниже минимальный happy path. Удобнее всего выполнять его через Swagger UI.

### 1. Создать проект

```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Vibe Code Manager",
    "rootDirectory": "/home/user/projects/vibe-code-manager"
  }'
```

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
    "workingDirectoryOverride": "/home/user/projects/vibe-code-manager"
  }'
```

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
