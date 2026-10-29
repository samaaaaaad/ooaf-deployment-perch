# Frontend (Vite + React)

The single-page app for customers, organisers and admins. It talks to the Spring Boot API only through `src/api`. In development, Vite proxies `/api` to `localhost:8080`. In production, the built files are bundled into the Spring Boot jar and served from the same origin.

```bash
npm ci
```

```bash
npm run dev
```

Other scripts: `npm test` runs Vitest once, `npm run test:watch` runs it in watch mode, and `npm run build` produces the production build in `dist/`.

## Structure

| Path | Role |
|---|---|
| `src/main.jsx`, `src/App.jsx` | Entry point and role-aware routing (signed out: landing and login; customers: browse, checkout and tickets; organisers: events, floor plans and venues; admins: dashboard) |
| `src/api/` | HTTP client and one module per backend resource ([README](src/api/README.md)) |
| `src/pages/` | Routed screens ([README](src/pages/README.md)) |
| `src/components/` | Shared UI ([README](src/components/README.md)) |
| `src/auth/AuthContext.jsx` | Signed-in user, kept in `localStorage` |
| `src/utils/` | `router.jsx` (a small History-API router), `eventDisplay.js` (API event to display fields, categories), `studentPricing.js`, `floorPlanDecor.js`, `formatCountdown.js`, `formatDate.js` |
| `src/styles.css`, `src/pages/*.css` | The app's own teal and gold design system, with no UI framework. Layouts collapse to one column on small screens |
| `public/events/` | Banner images referenced by the seeded events |
| `src/test/setup.js` | jest-dom matchers for Vitest |

## Conventions

- Components only call functions from `src/api`, never `fetch` directly.
- Every screen that loads data shows a loading state, a friendly error (with retry where it helps) and an empty state.
- Tests live next to the file they test (`*.test.jsx`) and mock `src/api` with `vi.mock`.
