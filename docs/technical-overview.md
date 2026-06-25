# Vibe Code Manager: техническое описание

Local-first приложение для отслеживания доступности AI coding tools и управления
отложенными очередями промптов.

Рабочие названия проекта:

- **Vibe Code Manager**
- **AI Limit Queue**
- CLI alias: `aiq`

> Local-first automation assistant for monitoring AI tool availability and
> managing deferred prompts.

## Статус проекта

Проект находится на раннем этапе разработки.

| Этап | Статус |
| --- | --- |
| Maven multi-module каркас | Готов |
| Domain Queue Core | Готов |
| Application use cases | Базово готово, продолжается расширение |
| PostgreSQL persistence | Готов |
| REST API | Базово готов |
| Fake AI adapter и Queue Runner | Базово готово |
| Codex CLI adapter | Базово готов |
| Claude Code adapter | Не начат |
| Уведомления | Не начат |
| React UI и Picocli CLI | Не начат |

Текущий backend успешно собирается, имеет REST API, PostgreSQL/Flyway
persistence, fake executor, базовый Codex CLI executor и тесты на основных
слоях. Продукт ещё не является функционально завершённым: уведомления,
scheduler, React UI, Picocli CLI и Claude Code adapter пока не реализованы.

Подробный план по fake, Codex CLI и Claude Code executors описан в
[`executors.md`](executors.md).

## Назначение

Пользователь может заранее создавать промпты для AI-инструментов, объединять
их в очереди и запускать после восстановления лимита инструмента.

Приложение должно:

1. Отслеживать доступность настроенных AI-инструментов.
2. Уведомлять пользователя об изменении состояния лимита.
3. Позволять создавать проекты, очереди и промпты.
4. Запускать очередь автоматически, после подтверждения или только уведомлять.
5. Сохранять историю запусков, stdout, stderr, raw output и exit code.
6. Работать локально и хранить данные в локальной PostgreSQL.
7. Поддерживать заменяемые адаптеры для AI CLI и каналов уведомлений.

## Ограничения

Приложение не предназначено для обхода ограничений AI-сервисов.

Запрещённые сценарии:

- обход rate limits;
- автоматизация мультиаккаунтов;
- автоматическое решение CAPTCHA;
- хранение паролей от аккаунтов;
- логирование токенов и других секретов;
- отправка приватных данных без явного действия пользователя.

Секреты должны передаваться через переменные окружения и не должны попадать в
Git, логи или доменную модель.

## Технологический стек

### Backend

- Java 21
- Spring Boot 4.x
- Maven multi-module
- Spring Web, Validation, Scheduler и Actuator
- Spring Data JPA
- PostgreSQL и Flyway
- JUnit 5, AssertJ, Mockito и Testcontainers

### Клиенты и интеграции

- React, TypeScript и Vite
- TanStack Query и React Router
- Picocli
- Telegram Bot API
- Desktop notifications
- Codex CLI и Claude Code CLI

## Архитектура

Проект строится как модульный монолит с использованием DDD, Hexagonal
Architecture и Ports and Adapters.

Направление зависимостей:

```text
domain <- application <- infrastructure/adapters/interfaces <- bootstrap
```

Основные правила:

- `domain` содержит только бизнес-модель и бизнес-правила;
- `application` координирует use cases и зависит только от `domain`;
- `infrastructure` реализует persistence, process runner, scheduler и Git;
- `adapters` реализуют интеграции с AI CLI и уведомлениями;
- `interfaces` предоставляют REST и Picocli интерфейсы;
- `bootstrap` собирает приложение и является единственным executable-модулем.

В `domain` запрещены Spring, JPA, REST DTO, `ProcessBuilder` и детали внешних
сервисов. JPA entities не должны использоваться как domain entities.

### Pragmatic Domain Model

В проекте используется прагматичный DDD, а не академическая модель с wrapper
классом для каждого примитива.

Для MVP **не нужно** создавать отдельные value objects только ради типизации:

- `ProjectId`;
- `PromptId`;
- `PromptQueueId`;
- `AiToolId`;
- `ProjectName`;
- `PromptTitle`;
- `PromptPriority`;
- `PromptPosition`;
- другие простые обёртки над `UUID`, `String`, `int` и `long`.

Вместо этого в aggregates допустимы обычные типы с понятными именами полей:

```java
UUID id;
UUID projectId;
UUID queueId;
String name;
String rootDirectory;
int priority;
long position;
```

Валидация таких полей должна жить в фабриках, конструкторах и доменных методах
самих aggregates.

Value object стоит вводить только если он реально упрощает модель:

- объединяет несколько связанных полей;
- содержит нетривиальное поведение;
- переиспользуется в нескольких местах;
- уменьшает дублирование валидации;
- делает невозможным опасное состояние.

Хорошие кандидаты:

- `WorkingHours`, потому что содержит `from`, `to`, `zoneId` и логику интервала
  через полночь;
- `QueueExecutionPolicy`, потому что объединяет правила запуска очереди;
- `ExecutionResult`, потому что объединяет stdout, stderr, raw output и exit
  code.

Плохие кандидаты для MVP:

- `record ProjectId(UUID value)`;
- `record ProjectName(String value)`;
- `record PromptPriority(int value)`.

Если простая проверка `not null`, `not blank`, `max length` используется только
в одном aggregate, её лучше оставить внутри этого aggregate.

## Структура репозитория

```text
.
├── backend/
│   ├── pom.xml
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   ├── adapters/
│   └── bootstrap/
├── docker-compose.yml
└── README.md
```

Текущие Maven-модули:

| Модуль | Ответственность |
| --- | --- |
| `domain` | Чистая бизнес-модель, события, политики и исключения |
| `application` | Use cases, commands/results и входные/выходные порты |
| `infrastructure` | PostgreSQL, Flyway, scheduler, process runner и Git |
| `adapters` | Агрегатор будущих AI CLI и notification adapters |
| `bootstrap` | Spring Boot entry point и runtime-конфигурация |

REST и Picocli interfaces будут добавлены отдельными модулями после
стабилизации application layer.

## Основная доменная модель

### Project

Рабочий проект пользователя:

- `UUID id`;
- имя;
- корневая директория;
- optional `codexSessionId` для привязки проекта к одному Codex context/session;
- статус `ACTIVE`, `DISABLED` или `ARCHIVED`.

Disabled project не участвует в автоматическом запуске. Archived project нельзя
изменять без отдельного сценария восстановления.

### AI Tool

Настройка внешнего AI-инструмента:

- `UUID id`;
- тип `CODEX`, `CLAUDE_CODE` или `CUSTOM`;
- executable path;
- command template;
- стратегия проверки лимита;
- enabled/disabled статус.

Disabled tool нельзя использовать для выполнения промптов.

### Prompt Queue

Очередь относится к проекту и хранит execution policy:

- `UUID id`;
- `UUID projectId`;
- название очереди;
- режим запуска `NOTIFY_ONLY`, `ASK_CONFIRMATION` или `AUTO_RUN`;
- максимальное число промптов за запуск;
- cooldown между выполнениями;
- `stopOnError`;
- разрешённые рабочие часы.

Статусы очереди:

```text
CREATED
WAITING_LIMIT
WAITING_CONFIRMATION
RUNNING
PAUSED
STOPPED
COMPLETED
DISABLED
```

### Prompt

Промпт относится к очереди и целевому AI-инструменту. Он содержит title,
content, priority, FIFO position, максимальное число попыток и опциональную
рабочую директорию.

Базовые поля:

- `UUID id`;
- `UUID queueId`;
- `UUID targetAiToolId`;
- `String title`;
- `String content`;
- `int priority`;
- `long position`;
- `int attemptCount`;
- `int maxAttempts`;

Статусы промпта:

```text
DRAFT
QUEUED
WAITING_LIMIT
WAITING_CONFIRMATION
RUNNING
COMPLETED
FAILED
CANCELLED
SKIPPED
```

Порядок выполнения:

1. Только промпты со статусом `QUEUED`.
2. Более высокий priority выполняется раньше.
3. При равном priority используется меньшая FIFO position.
4. При равной position используется более ранний `createdAt`.

### Prompt Execution

Каждая попытка запуска промпта является отдельным execution. Для неё
сохраняются:

- фактически выполненная команда;
- stdout, stderr и raw output;
- exit code и сообщение об ошибке;
- started/finished timestamps и duration;
- позднее: Git status и diff.

Для execution допустимы обычные `UUID promptId`, `UUID aiToolId`, `String
command`, `int exitCode`. Отдельный `ExecutionResult` полезен, потому что
группирует несколько полей результата.

### Limit и Notification

Проверка лимита возвращает `AVAILABLE`, `LIMITED`, `UNKNOWN` или `ERROR`.

Уведомления не зависят от Telegram или desktop API в domain layer. Конкретный
канал выбирается через application port и adapter.

## Требования к Queue Runner

Queue Runner является основной application orchestration-службой. Он должен:

1. Загрузить очередь и проверить её состояние.
2. Проверить working hours и safety policy.
3. Выбрать следующий queued prompt.
4. Проверить доступность целевого AI-инструмента.
5. При необходимости ожидать лимит или подтверждение пользователя.
6. Создать execution и перевести prompt в `RUNNING`.
7. Выполнить prompt через подходящий adapter.
8. Сохранить результат и обновить состояния prompt и queue.
9. Остановить очередь при ошибке, если включён `stopOnError`.
10. Отправить уведомления и продолжить согласно execution policy.

Длительное ожидание и cooldown не должны блокировать HTTP-поток.

До реализации Queue Runner необходимо определить:

- use case и API подтверждения запуска;
- защиту от одновременного запуска одной очереди;
- восстановление `RUNNING` состояний после перезапуска;
- приоритет между `SafetyPolicy` и `AutoRunMode`.

## План разработки

### Milestone 1: Maven skeleton

Статус: **готов**.

- создать Maven parent;
- подключить `domain`, `application`, `infrastructure`, `adapters`, `bootstrap`;
- настроить направление зависимостей;
- оставить `bootstrap` единственным executable-модулем;
- обеспечить успешный `mvn clean verify`.

### Milestone 2: Domain Queue Core

Статус: **готов**.

- реализовать `DomainException`, `DomainEvent` и `AggregateRoot`;
- реализовать простые domain entities для project, prompt и queue;
- использовать `UUID`, `String`, `int`, `long`, `Instant`, `Duration` напрямую,
  если отдельный value object не даёт явной пользы;
- реализовать `WorkingHours` и `QueueExecutionPolicy` как настоящие value
  objects, потому что они содержат связанные поля и поведение;
- реализовать queue enums;
- реализовать `Prompt` и его переходы состояний;
- реализовать `PromptQueue` и его переходы состояний;
- реализовать `NextPromptSelector` и `PromptOrderingService`;
- покрыть бизнес-правила unit-тестами.

Для этого milestone не нужно создавать wrapper-классы вида `PromptId`,
`QueueName`, `PromptContent`, если валидация используется только внутри одного
aggregate.

Критерий готовности:

```bash
cd backend
mvn -pl domain test
mvn clean verify
```

### Milestone 3: Application use cases

- определить repository и integration ports;
- добавить immutable commands и results;
- реализовать создание проекта, инструмента, очереди и промпта;
- реализовать базовый запуск очереди с in-memory repositories;
- использовать fake executor и fake limit checker;
- покрыть application services тестами.

### Milestone 4: PostgreSQL persistence

- создать Flyway migrations;
- создать отдельные JPA entities;
- реализовать Spring Data repositories;
- реализовать persistence mappers и repository adapters;
- добавить optimistic locking и необходимые индексы;
- проверить migrations и adapters через Testcontainers.

### Milestone 5: REST API

- добавить отдельный REST interfaces-модуль;
- реализовать controllers, request/response DTO и mappers;
- добавить Jakarta Validation и global exception handler;
- реализовать endpoints для projects, tools, queues, prompts и executions;
- проверить полный сценарий через интеграционный тест.

### Milestone 6: Fake AI adapter

- возвращать fake stdout и успешный result;
- уметь симулировать execution failure;
- уметь симулировать исчерпанный лимит;
- использовать adapter для разработки Queue Runner и REST без внешних CLI.

### Milestone 7: Queue Runner и scheduler

- реализовать основную orchestration-службу;
- поддержать max prompts per run, cooldown и stop on error;
- реализовать working hours и confirmation flow;
- защитить очередь от конкурентного запуска;
- добавить limit-check и auto-run schedulers;
- реализовать восстановление после перезапуска.

### Milestone 8: Реальные AI CLI adapters

- создать общий безопасный `ProcessRunner`;
- реализовать Codex CLI adapter;
- реализовать Claude Code CLI adapter;
- добавить command builders, output parsers и limit parsers;
- покрыть парсеры fixture-тестами.

Команды должны передаваться в `ProcessBuilder` списком аргументов. Выполнение
произвольной shell-строки запрещено по умолчанию.

### Milestone 9: Notifications

- реализовать notification application services;
- реализовать Telegram sender;
- реализовать desktop notifications;
- сохранять notification events и повторять неуспешные доставки;
- позднее добавить Telegram-команды и action buttons.

### Milestone 10: React UI

- Dashboard;
- Projects и Project Details;
- AI Tools;
- Queue и Prompt Details;
- Executions;
- Settings.

### Milestone 11: Picocli interface

- добавить команды `status`, `project`, `tool`, `queue`, `prompt`, `run` и
  `config`;
- использовать те же application use cases, что REST и Telegram.

### Milestone 12: Safety improvements

- dry-run;
- сохранение Git status и diff;
- подтверждение перед изменениями файлов;
- advanced mode для custom command templates;
- опциональный auto commit, отключённый по умолчанию.

## MVP

MVP считается готовым, когда пользователь может:

1. Создать проект.
2. Зарегистрировать конфигурации Codex и Claude Code.
3. Создать очередь и добавить промпты.
4. Использовать priority и FIFO ordering.
5. Запустить очередь вручную.
6. Остановить очередь при ошибке prompt.
7. Просмотреть сохранённый execution result.
8. Проверить лимит хотя бы через manual/mock strategy.
9. Отправить Telegram-уведомление.
10. Управлять core-сценариями через REST API.

Дополнительно должны выполняться:

- PostgreSQL хранит все core entities;
- проходят domain и application tests;
- проект запускается локально через документированные команды.

## Планируемый REST API

Основные группы endpoints:

```text
/api/projects
/api/ai-tools
/api/queues
/api/prompts
/api/executions
/api/notification-channels
/api/settings
```

REST controllers должны только валидировать и преобразовывать запросы, после
чего вызывать application use cases. Бизнес-логика в controllers запрещена.

## Правила разработки

1. Не добавлять Spring и JPA annotations в `domain`.
2. Не использовать JPA entities как domain entities.
3. Не размещать бизнес-правила в controllers и application orchestration.
4. Использовать явные domain methods вместо публичных setters.
5. Не создавать wrapper value objects для каждого примитива. Использовать
   `UUID`, `String`, `int`, `long` напрямую, если это проще и безопасно.
6. Использовать value object только для настоящей группы данных или поведения:
   `WorkingHours`, `QueueExecutionPolicy`, `ExecutionResult`.
7. Использовать `Instant` для timestamps и `Duration` для интервалов.
8. Использовать `LocalTime` и `ZoneId` для working hours.
9. Прятать внешние интеграции за application ports.
10. Писать domain tests до persistence и REST.
11. Не логировать секреты.
12. Не начинать реальный CLI adapter до прохождения domain/application tests.
13. Предпочитать простую модель, которую легко читать и менять, сложной модели
    с большим числом одноразовых классов.

## Локальная разработка

Требования:

- Java 21;
- Maven 3.9+;
- Docker с Docker Compose.

Проверить весь backend:

```bash
cd backend
mvn clean verify
```

Проверить только domain:

```bash
cd backend
mvn -pl domain test
```

Запустить PostgreSQL:

```bash
docker compose up -d postgres
```

Остановить PostgreSQL:

```bash
docker compose down
```

Текущая конфигурация PostgreSQL:

```text
database: aiqueue
user:     aiqueue
password: aiqueue
port:     5432
```

Эти значения предназначены только для локальной разработки. Для запуска backend
нужно передать `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` и
`SPRING_DATASOURCE_PASSWORD` либо добавить эквивалентные настройки в Spring
configuration.

## Тестовая стратегия

### Domain tests

- разрешённые и запрещённые переходы состояний prompt и queue;
- retry attempts;
- priority и FIFO ordering;
- working hours, включая интервал через полночь;
- execution lifecycle.

### Application tests

- use cases с mock/in-memory outbound ports;
- успешный запуск queue;
- failure, limit, confirmation и stop-on-error scenarios.

### Infrastructure tests

- Flyway migrations;
- persistence mappers;
- repository adapters с PostgreSQL Testcontainers.

### Adapter и REST tests

- command builders и output/limit parsers;
- REST validation и error handling;
- полный happy-path сценарий с mock AI adapter.

## Ближайшая задача

Продолжить развитие runtime-сценариев поверх готового backend skeleton:

- стабилизировать Queue Runner и scheduler;
- добавить проверку лимитов и notification flow;
- реализовать Claude Code adapter;
- добавить Telegram/Desktop notifications;
- расширить end-to-end сценарии через REST API.
