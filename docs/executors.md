# Executor Implementation Plan

## Назначение

Executor отвечает за запуск одного prompt во внешнем AI-инструменте и возврат
`ExecutionResult`. Он не должен выбирать следующий prompt, менять статусы queue
или решать retry-policy. Эти решения остаются в application/domain слоях.

Текущий application port:

- `PromptExecutor.buildCommand(PromptExecutionRequest request)` - строит
  человекочитаемую команду для сохранения в `PromptExecution`;
- `PromptExecutor.execute(PromptExecutionRequest request)` - запускает prompt и
  возвращает `ExecutionResult`.

## Уже реализовано

### FakePromptExecutor

Файл: `backend/infrastructure/src/main/java/com/aiq/infrastructure/executor/FakePromptExecutor.java`

Поведение:

- включён как Spring bean по умолчанию;
- отключается через `aiq.executor.fake.enabled=false`;
- `buildCommand` возвращает строку вида
  `fake-executor --ai-tool-id <id> --prompt-id <id>`;
- обычный prompt возвращает успешный `ExecutionResult` с `exitCode = 0`;
- prompt, в тексте которого есть `[fake-fail]`, возвращает ошибочный
  `ExecutionResult` с `exitCode = 1`.

Назначение fake executor:

- быстро тестировать `QueueRunnerApplicationService`;
- проверять `stopOnError`;
- запускать REST/UI сценарии без реальных CLI;
- не зависеть от локально установленного Codex CLI или Claude Code CLI.

## Общие требования ко всем executors

1. Executor получает только `PromptExecutionRequest`.
2. Executor возвращает только `ExecutionResult`.
3. Executor не сохраняет domain entities в repositories.
4. Executor не меняет статусы `Prompt`, `PromptQueue`, `PromptExecution`.
5. Executor не делает retry. Retry решает `QueueRunnerApplicationService`.
6. Executor не проверяет дневные/месячные лимиты. Для этого нужен отдельный
   application port, например `AiLimitChecker`.
7. Executor не запускает shell-команду строкой.
8. Реальные CLI команды должны передаваться в `ProcessBuilder` списком
   аргументов.
9. Secrets нельзя логировать и нельзя сохранять в `PromptExecution.command`.
10. `stdout`, `stderr`, `rawOutput`, `exitCode`, `errorMessage` должны
    сохраняться достаточно полно для диагностики.

## Общая инфраструктура для реальных CLI

Перед `CodexCliPromptExecutor` и `ClaudeCodePromptExecutor` стоит сделать общий
`ProcessRunner`.

Рекомендуемые классы:

- `ProcessCommand`
  - `List<String> arguments`;
  - `Path workingDirectory`;
  - `Map<String, String> environment`;
  - `Duration timeout`;
  - `int maxOutputBytes`.
- `ProcessRunResult`
  - `int exitCode`;
  - `String stdout`;
  - `String stderr`;
  - `Duration duration`;
  - `boolean timedOut`.
- `ProcessRunner`
  - `ProcessRunResult run(ProcessCommand command)`.

Требования к `ProcessRunner`:

- использовать Java `ProcessBuilder`;
- не использовать `sh -c`, `bash -c`, `cmd /c` для обычных запусков;
- читать `stdout` и `stderr` параллельно, чтобы процесс не завис на полном
  buffer;
- поддерживать timeout и завершение процесса;
- ограничивать максимальный размер output;
- нормализовать exit code и timeout в понятный результат;
- логировать только безопасные поля;
- не печатать токены, ключи, cookies и содержимое env.

## Конкретные реализации

### 1. FakePromptExecutor

Статус: реализован.

Дополнительные сценарии, которые можно добавить позже:

- `[fake-timeout]` - симуляция timeout;
- `[fake-limit]` - симуляция исчерпанного лимита, но лучше через отдельный
  `AiLimitChecker`;
- configurable delay для проверки scheduler/async flow.

Тесты:

- успешный prompt возвращает `exitCode = 0`;
- prompt с `[fake-fail]` возвращает `exitCode = 1`;
- `buildCommand` возвращает непустую команду с prompt id и AI tool id.

### 2. CodexCliPromptExecutor

Назначение: запускать prompt через локальный Codex CLI.

Перед реализацией нужно зафиксировать:

- где хранится путь к executable;
- как выбирается model/profile/config;
- передаётся ли prompt через stdin, временный файл или аргумент CLI;
- какие exit codes считаются системной ошибкой, а какие ошибкой prompt;
- как CLI сообщает об исчерпанном лимите;
- какие флаги CLI актуальны в текущей версии.

Важно: точные флаги Codex CLI надо проверять перед реализацией через
`codex --help` или официальную документацию OpenAI. Не стоит зашивать их по
памяти, потому что CLI может меняться.

Ожидаемые компоненты:

- `CodexCliProperties`
  - `Path executablePath`;
  - `Duration timeout`;
  - `List<String> defaultArguments`;
  - `int maxOutputBytes`;
  - `boolean enabled`.
- `CodexCommandBuilder`
  - строит `ProcessCommand`;
  - передаёт prompt безопасным способом;
  - добавляет working directory;
  - не использует shell escaping.
- `CodexOutputParser`
  - выделяет полезный stdout;
  - выделяет error message;
  - позже распознаёт limit/usage сообщения.
- `CodexCliPromptExecutor`
  - реализует `PromptExecutor`;
  - вызывает `ProcessRunner`;
  - переводит `ProcessRunResult` в `ExecutionResult`.

Минимальный acceptance criteria:

- можно выполнить prompt в project root;
- команда не раскрывает secrets;
- timeout переводится в failed `ExecutionResult`;
- stderr сохраняется;
- parser покрыт fixture-тестами;
- runner tests проходят с mock и с fake `ProcessRunner`.

### 3. ClaudeCodePromptExecutor

Назначение: запускать prompt через локальный Claude Code CLI.

Перед реализацией нужно зафиксировать:

- точный executable name и путь;
- актуальные CLI-флаги через `claude --help` или документацию Anthropic;
- способ передачи prompt;
- формат успешного output;
- формат limit/error сообщений;
- особенности interactive/approval режима.

Ожидаемые компоненты:

- `ClaudeCodeProperties`
  - `Path executablePath`;
  - `Duration timeout`;
  - `List<String> defaultArguments`;
  - `int maxOutputBytes`;
  - `boolean enabled`.
- `ClaudeCodeCommandBuilder`
  - строит `ProcessCommand`;
  - не использует shell;
  - учитывает working directory.
- `ClaudeCodeOutputParser`
  - нормализует stdout/stderr;
  - выделяет user-facing error message;
  - позже выделяет limit status.
- `ClaudeCodePromptExecutor`
  - реализует `PromptExecutor`;
  - вызывает `ProcessRunner`;
  - возвращает `ExecutionResult`.

Минимальный acceptance criteria:

- prompt запускается из нужного project root;
- failed exit code сохраняется как failed `ExecutionResult`;
- timeout не оставляет висящий процесс;
- parser покрыт fixture-тестами;
- нет запуска через shell string.

## Limit checker

Лимиты лучше не смешивать с executor.

Отдельный port:

- `AiLimitChecker.checkLimit(UUID aiToolId)` или
  `AiLimitChecker.checkLimit(AiLimitCheckRequest request)`;
- результат: `AVAILABLE`, `LIMIT_REACHED`, `UNKNOWN`, возможно `resetAt`.

Отдельные реализации:

- `FakeAiLimitChecker`;
- `CodexCliLimitChecker`;
- `ClaudeCodeLimitChecker`;
- позднее API/provider-specific checkers.

`QueueRunnerApplicationService` должен проверять limit перед запуском prompt и
переводить queue/prompt в `WAITING_LIMIT`, если лимит исчерпан.

## Рекомендуемые библиотеки и фреймворки

Основные:

- Java 21;
- Spring Boot для DI, profiles, configuration properties;
- `ProcessBuilder` и `ProcessHandle` из JDK для запуска CLI;
- SLF4J/Logback из Spring Boot для логирования;
- Micrometer + Spring Boot Actuator для метрик.

Тесты:

- JUnit 5;
- AssertJ;
- Mockito;
- Awaitility для timeout/async сценариев, когда появится scheduler;
- Testcontainers только для persistence/integration tests, не для unit-тестов
  command builders.

Конфигурация:

- Spring `@ConfigurationProperties` для executor settings;
- Jakarta Validation для проверки properties после подключения REST/bootstrap
  validation;
- Spring profiles/properties для выбора active executor:
  - `aiq.executor.fake.enabled=true`;
  - `aiq.executor.codex.enabled=true`;
  - `aiq.executor.claude.enabled=true`.

Что не использовать на первом этапе:

- Apache Commons Exec, пока хватает `ProcessBuilder`;
- shell scripts как обязательную прослойку;
- retries внутри executor;
- сложный workflow engine.

## Порядок реализации

1. Закрыть fake executor тестами.
2. Добавить in-memory repositories для application tests.
3. Покрыть `QueueRunnerApplicationService` тестами на success, failure,
   stop-on-error и max prompts.
4. Добавить `ProcessRunner`.
5. Добавить fake process runner tests без реального CLI.
6. Реализовать `CodexCommandBuilder` и fixture-тесты.
7. Реализовать `CodexCliPromptExecutor`.
8. Реализовать `ClaudeCodeCommandBuilder` и fixture-тесты.
9. Реализовать `ClaudeCodePromptExecutor`.
10. Вынести limit check в отдельный port и подключить его к runner.
11. Добавить scheduler/auto-run только после стабильных executor tests.

## Definition of Done

- `mvn clean verify` проходит;
- fake executor работает без внешних CLI;
- реальные executors не используют shell string;
- command builders покрыты тестами;
- output parsers покрыты fixture-тестами;
- таймауты не оставляют дочерние процессы;
- secrets не попадают в logs и `PromptExecution.command`;
- executor можно выбрать через Spring configuration.
