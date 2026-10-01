# DocTutorials CRM — monorepo

```
project/
├── backend/    Spring Boot 3 REST API (Java 21) — see backend/README.md, backend/FEATURES.md
└── frontend/   React + Vite + TypeScript SPA (this is the new part)
```

## Running locally

**Backend** (port 8080 by default — see `backend/README.md` / `backend/docker-compose.yml`):
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend** (port 3000, proxies `/api` to `http://localhost:8080` in dev — see `frontend/vite.config.ts`):
```bash
cd frontend
npm install
npm run dev
```

No `.env` is required for local dev. For a client deployment, set `VITE_API_BASE_URL` at build time
to that client's backend origin (see `frontend/.env.example`) — nothing else needs to change per client.

## What's implemented vs. placeholder

The frontend was built directly against the real backend contract (every request/response type in
`frontend/src/types/` is hand-matched to the actual Java DTOs/entities, not guessed). Sections of the
UI that the client's screenshots show but the backend doesn't support yet are rendered as a clearly
labeled "Coming soon" block (with the FEATURES.md phase) instead of being faked with mock data —
search the frontend for `ComingSoon` to see every one, and for `NOTE:` comments in `frontend/src/api/`
for the specific backend gaps (e.g. no global `GET /calls` or `GET /tasks` list endpoint yet, no
`AuditController` exposing the audit log that's already being written).

That's a deliberate choice for a project heading toward 100+ features across multiple clients: it
keeps the frontend honest about what's real today, and gives every planned feature an obvious slot to
wire up later without a UI rebuild.

## Frontend stack

Vite + React 18 + TypeScript, Tailwind CSS + shadcn/ui-style primitives (`src/components/ui`),
TanStack Query for server state, Zustand for auth/session state, Axios with automatic JWT refresh
(`src/api/client.ts`), react-router-dom v6, zod for the two forms where bad input is costly
(auth + lead create/edit).

Structure:
```
frontend/src/
├── api/          one file per backend resource, thin wrappers around apiClient
├── components/
│   ├── ui/       shadcn-style primitives (button, dialog, table, tabs, ...)
│   ├── layout/   Sidebar, Topbar, AppLayout
│   ├── leads/    lead-specific widgets (badges, form dialog, overview tab, assign dialog)
│   ├── tasks/ calls/ notes/ documents/   per-lead tab components, reused on the Tasks/Calls pages too
│   └── common/   EmptyState, ComingSoon, Pagination, PageHeader
├── pages/        one file per route
├── routes/       ProtectedRoute (auth + role gating)
├── store/        authStore (zustand, persisted session)
├── types/        TypeScript types matched field-for-field to backend DTOs/entities
└── lib/          cn(), formatters, zod schemas, query client
```
