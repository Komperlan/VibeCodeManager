# Vibe Code Manager

Local-first backend для управления проектами, AI-инструментами, очередями
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
- Swagger UI доступен после запуска приложения.

## Требования

- Java 21
- Maven 3.9+
- Docker и Docker Compose

## Быстрый запуск

Запустить PostgreSQL:

```bash
docker compose up -d postgres
```

Запустить backend:

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

Остановить PostgreSQL:

```bash
docker compose down
```

## Проверка проекта

Запустить все тесты:

```bash
mvn -f backend/pom.xml test
```

Infrastructure и bootstrap tests используют Testcontainers, поэтому для полного
прогона должен быть доступен Docker.

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
