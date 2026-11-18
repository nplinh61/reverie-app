# Reverie Client

Angular frontend for Reverie, shared across web and mobile. This is part of the
Milestone 0 technical foundation.

## Technology

- Angular 22 with standalone components
- Ionic 9 for UI components
- Capacitor 8 for mobile builds (`appId` `com.reverie.app`)
- TypeScript
- Vitest (jsdom) for unit tests, run through the Angular build system
- npm (pinned through `packageManager` in `package.json`)

## Current state (Milestone 0)

The client is a scaffold with a single working feature: it checks whether it can
reach the backend. A `ConnectionStatus` component asks `HealthService`, which
calls `GET /api/v1/ping`, and shows a loading, connected, or error state. No
domain screens exist yet, by design.

## Development server

Run from this directory (`apps/client`):

```bash
npm start
```

This runs `ng serve`. Open `http://localhost:4200/`. The app reloads when you
change source files. To see the connection status as "connected", the backend
must also be running; see the repository root README and `apps/api/README.md`.

## Backend API connection

The client talks to the Reverie backend through a single base URL defined in the
Angular environment files:

- `src/environments/environment.development.ts` sets `apiBaseUrl` to
  `http://localhost:8080`, the backend's default port during local development.
  Because the dev server runs on `http://localhost:4200`, API calls cross origins
  and exercise the backend CORS allowlist on purpose.
- `src/environments/environment.ts` (used by production builds) sets `apiBaseUrl`
  to an empty string. An empty value means the client expects the API at the
  **same origin**, served behind a reverse proxy that routes `/api/**` to the
  backend.

All HTTP access goes through services under `src/app/core/` (for example
`HealthService`, which calls `GET /api/v1/ping`); components never build URLs or
call `HttpClient` directly.

### Deployment requirement

The empty production `apiBaseUrl` is a placeholder, not a working value. Any real
deployment **must** either set `apiBaseUrl` to the backend's absolute URL or
provide an equivalent reverse-proxy or ingress configuration that serves the API
on the same origin as the frontend. Running a local backend to see the connection
status is described in the repository root documentation.

## Build

```bash
npm run build
```

This runs `ng build`, which defaults to the production configuration and enforces
the bundle size budgets. Output goes to `dist/client/browser`, which is also the
`webDir` that Capacitor packages for mobile.

## Unit tests

```bash
npm test
```

This runs the Vitest unit tests once (jsdom environment). There are specs for the
app, `HealthService`, and `ConnectionStatus`. There is no end-to-end test setup
in Milestone 0.

## Continuous integration


Milestone 0 is complete and merged into `main`. Backend and frontend
checks have passed in GitHub Actions CI.

See the [Development Roadmap](../../docs/development/ROADMAP.md)
for milestone status and planned work.
