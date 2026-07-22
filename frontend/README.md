# ClaimLens Frontend

Next.js 16 (App Router) UI for the ClaimLens motor-insurance claims platform. One app whose
navigation and landing page adapt to the signed-in user's permissions — that is how the six "portals"
(admin, product manager, claims/investigation manager, investigator, customer support, auditor/analyst)
are realised, rather than six separate apps.

## Stack

- **Next.js 16** App Router, TypeScript (strict)
- **Tailwind CSS v4** + **shadcn/ui** (Radix base) — small composable components
- **Redux Toolkit** for app state (auth/session)
- **axios** single instance (`lib/http.ts`) — attaches the Bearer token, transparently refreshes an
  expired access token once, and normalises backend errors
- **react-hook-form** for forms
- **dayjs** (single instance, `lib/dayjs.ts`)
- **next-themes** — system / light / dark
- **sonner** for toasts, **lucide-react** icons

## Layout

```
app/
  login/                 public sign-in
  (app)/                 authenticated shell (sidebar + topbar), route-guarded
    dashboard, claims, customers, policies, products, rulesets,
    organization/{regions,departments,designations,companies}, notifications
components/
  ui/                    shadcn primitives
  claims/, customers/, policies/, products/, rulesets/, organization/   feature components
  data-table, status-badge, page-header, can, theme-toggle, ...         shared
services/                one file per API domain (thin typed wrappers over lib/http)
store/                   redux slices (authSlice)
hooks/                   redux, useAuth, useAsync, useMutation
lib/                     http, dayjs, types, enums, permissions, navigation, format, utils
```

## Auth & permissions

- On login, tokens go to `localStorage`; `GET /auth/me` returns the resolved permission codes.
- The JWT deliberately carries only `roleId` (revocation takes effect immediately server-side), so the
  UI gates on the `/auth/me` permission list — never on the token. Use `<Can permission="...">` or the
  `useAuth().has(...)` helper.

## Develop

```bash
npm install
npm run dev          # http://localhost:3000  (expects the backend at :8080)
npm run build        # production build
npm run typecheck    # tsc --noEmit
npm run format       # prettier
```

Set the API base URL in `.env.local` (defaults to `http://localhost:8080/api/v1`):

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```
