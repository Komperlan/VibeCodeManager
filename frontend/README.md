# Vibe Code Manager Frontend

React + TypeScript UI по Figma Make макету для local-first менеджера очередей AI prompt-ов.

## Запуск

```bash
cd frontend
npm install
npm run dev
```

По умолчанию UI работает на mock data. Для будущего Spring Boot backend есть HTTP API слой в `src/api.ts`.

```bash
VITE_USE_MOCK_API=false VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev
```

With VPN enabled, prefer opening the UI at `http://127.0.0.1:5173` and keep
`VITE_API_BASE_URL=http://127.0.0.1:8080`.

In HTTP mode, projects, AI tools, queues and prompts are loaded from the backend.
Settings and limit-check actions are local frontend fallbacks until dedicated
Spring Boot endpoints are implemented.

## Страницы

- `Dashboard` - общий статус, запуск очереди, limit checker.
- `Projects` - локальные workspace root directories.
- `AI Tools` - Codex, Fake, Claude Code и custom executors.
- `Queues` - политики запуска, прогресс и backlog prompt-ов.
- `Settings` - mock/runtime настройки фронта.

Tailwind подключён через CDN fallback, чтобы проект собирался даже без локальной установки Tailwind-пакетов.
