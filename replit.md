# Auto Reply Tools

Native Android automation that turns approved Viber notifications into safe,
state-driven replies through a configured AI app.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- Required env: `DATABASE_URL` — Postgres connection string

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

## Where things live

- `android/` — native Kotlin application and Android service configuration.
- `android/app/src/main/java/com/autoreplytools/core/` — automation state machine,
  semantic accessibility matching, adapters, safety controls, and models.
- `android/app/src/main/java/com/autoreplytools/service/` — Android framework
  service entry points for accessibility and Viber notifications.
- `android/README.md` — Android build and verification boundary.
- `.conversation/attached_assets/replit_build_1789245353994.txt` — supplied
  master specification.

## Architecture decisions

- The automation engine owns state transitions and never embeds Viber or AI
  selectors.
- Accessibility nodes are selected by semantic properties and confidence
  scoring; bounds and coordinates are not used for actions.
- Notification-provided conversation intents are preferred. If a target
  conversation cannot be opened safely, the run fails without sending.
- Every stage uses bounded polling, a timeout, and a controlled failure path.
- Persistent user settings use Android DataStore Preferences; transient
  duplicate protection remains in memory by design.

## Product

Users can enable automatic replies, configure an approved-sender whitelist,
choose the Android AI app adapter, grant the two required system permissions,
observe the current automation state, and stop/resume automation immediately.

## User preferences

- Follow the supplied master prompt as the source of product and engineering
  requirements.

## Gotchas

- Android AccessibilityService and NotificationListenerService behavior must be
  verified on a real Android device or emulator; a successful Gradle build is
  not feature verification.
- Do not replace semantic matching with coordinates or a single fragile
  resource ID.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
