# EdgeMania.io — Development Roadmap

**Related:** [SPEC.md](SPEC.md) (technical), [DESIGN.md](DESIGN.md) (theme/layout)

Milestones are ordered to deliver a runnable vertical slice early and stack features incrementally. Each milestone ends in a demoable state and a green `mvn test`.

---

## Milestone M1 — Project Scaffold + Design Tokens

**Goal:** Repo builds and serves a themed shell; tokens in place.

### Tasks
- [x] `pom.xml` (Spring Boot 3.3.x, Java 21, `starter-web`, `starter-validation`, `starter-test`).
- [x] `.gitignore` (target/, IDE files).
- [x] `EdgeManiaApplication`, `WebConfig` (static fallback, `/api/**` separation).
- [x] `static/css/theme.css` — every token from DESIGN.md §2–§5 as custom properties.
- [x] `static/css/base.css` — reset, typography (`Hanken Grotesk`/`JetBrains Mono`), layout primitives, hatch + drop-stroke + wobble utilities.
- [x] `static/css/components.css` — buttons, inputs, panels, toggles, checkboxes/radios per DESIGN.md §6.
- [x] Shared nav + footer on a bare `index.html`.
- [x] Brand logo mark as inline SVG in `assets/brand/`.
- [x] `ErrorResponse`, `GlobalExceptionHandler`, `ApiException`.

### Acceptance Criteria
- [x] `mvn clean spring-boot:run` serves `http://localhost:8080` with a themed shell (hatched surfaces, ink borders, drop-stroke buttons).
- [x] All component styles (button states, inset inputs, manga panels, checkboxes/radios, toggles) render per DESIGN.md.
- [x] No stray hex colors in component files — only `theme.css` variables.
- [x] `prefers-reduced-motion` respected; focus rings visible.
- [x] `mvn test` green (exception-handler tests).

---

## Milestone M2 — Welcome Page

**Goal:** Landing page complete and on-brand.

### Tasks
- [x] `index.html` per DESIGN.md §7.1: nav, hero, 4 feature manga-panels, footer.
- [x] Hero background hatch texture + decorative node-graph SVG.
- [x] Wire nav links (Playground, Dashboard) — target pages may 404 until M3/M4 (acceptable interim).
- [x] Responsive behavior ≥1024px.

### Acceptance Criteria
- [x] Visual match to DESIGN.md §7.1 wireframe.
- [x] All CTAs navigate correctly; hover/active drop-stroke shift works.
- [x] Page passes the DESIGN.md §9 accessibility checks (contrast, keyboard, semantics).

---

## Milestone M3 — Playground + Node Configuration

**Goal:** Core editor: palette → canvas → drawer; runnable simulation.

### Tasks
- Backend:
  - [x] `NodeType`/`Node`/`Edge`/`Graph`/`SimulationRun` models + `SampleData` seed (3 types: device, edge, cloud).
  - [x] `NodeCatalogService` + `NodeCatalogController` (GET/POST/PUT/DELETE per SPEC §5.1).
  - [x] `SimulationService` + `SimulationController` (run engine: N ticks, propagate values across edges; GET run detail per SPEC §5.2).
  - [x] DTO records + Bean Validation (SPEC §5.4).
- Frontend:
  - [x] `playground.html` + `graph.js` + `nodes.js` (canvas, palette, connections, grid snap, selection).
  - [x] Property drawer (Node Configuration) — field rendering per node-type schema, draggable numerics with value-fill, save → `PUT /api/nodes/{id}`.
  - [x] Toolbar: New / Load Sample / Run; status footer after run.
  - [x] Wavy ink edges (SVG), hex sockets, scribbled node headers.

### Acceptance Criteria
- [x] Drag from palette → node created; drag moves (4px snap); sockets connect wavy lines; `Delete` removes selection.
- [x] Selecting a node loads its config in the drawer; edits persist via API and re-render.
- [x] Run completes → `status: COMPLETED` and per-node last values shown (device→edge→cloud pipeline).
- [x] Controller tests green (create/validation/404/409 cases from SPEC §8.2).

---

## Milestone M4 — Security Dashboard

**Goal:** Live metrics, charts, and event feed.

### Tasks
- Backend:
  - [ ] `MetricSnapshot`/`MetricPoint` models; `DashboardService` (seeded + rolling telemetry generator).
  - [ ] `DashboardController` — `GET /api/dashboard/metrics` and `/metrics/history?window=` (SPEC §5.3).
- Frontend:
  - [ ] `dashboard.html` + `dashboard.js` — KPI tiles, SVG sparkline/area charts (no library), event feed.
  - [ ] 5s polling with `visibilitychange` pause; last-refresh footer.
  - [ ] Severity color-coding per DESIGN.md §2.

### Acceptance Criteria
- [ ] Tiles + charts + feed render from API; values in JetBrains Mono.
- [ ] Polling updates data every 5s; pauses when tab hidden; no console errors.
- [ ] Charts drawn with ink grid lines + hatch fill per DESIGN.md §6.7.
- [ ] Dashboard controller tests green.

---

## Milestone M5 — Integration, Polish & Hardening

**Goal:** Site-wide consistency and release readiness.

### Tasks
- [ ] Full manual checklist from SPEC §8.3 across all pages.
- [ ] Cross-page consistency audit against DESIGN.md (borders, hatching, drop strokes, typography).
- [ ] Empty/loading/error states for every async surface (API down, empty graph, empty feed).
- [ ] Keyboard navigation audit; `aria-label`s; reduced-motion final pass.
- [ ] Edge cases: duplicate connections, self-loops, unknown node ids in graph (409), oversized `ticks` (400).
- [ ] `mvn clean package` → single runnable JAR; smoke-test from JAR.

### Acceptance Criteria
- [ ] `mvn test` fully green; manual checklist passes end-to-end.
- [ ] All four pages navigate cleanly; no 404s on nav; no console errors.
- [ ] Error envelopes (SPEC §5.5) rendered nicely in UI (toast/banner in theme).
- [ ] JAR build works from a clean checkout.

---

## Milestone M6 — Local Save/Load (.em Files)

**Goal:** Users can save their Playground graph to a local `.em` file and reload it — backend-built, backend-parsed.

### Tasks
- Backend:
  - [ ] `GraphFileService` — serialize a graph into the canonical `.em` JSON (format/version/name/savedAt envelope), parse uploaded files with Jackson, filename sanitization (SPEC §5.6).
  - [ ] `GraphController` — `POST /api/graphs/export` (returns `.em` attachment with `Content-Disposition` filename) and `POST /api/graphs/import` (multipart `.em` → graph JSON).
  - [ ] Import validation: extension `.em`, JSON parse, `format`/`version`, duplicate ids, edge→node/socket references, unknown `typeId` (all → 400, SPEC §5.6).
  - [ ] Multipart config in `application.properties` (5MB / 6MB limits).
  - [ ] `GraphControllerTest` (SPEC §8.2).
- Frontend:
  - [ ] Toolbar **Save ▾** → naming modal (DESIGN §6.8) → `POST /api/graphs/export` → blob download as `<name>.em`; disabled on empty canvas.
  - [ ] Toolbar **Load** → hidden `<input type="file" accept=".em">` → client-side extension check → `POST /api/graphs/import` → replace canvas (replace-confirm modal when non-empty), re-render nodes/edges, clear selection + drawer.
  - [ ] Themed error banner for backend import failures (bad file, unknown type, invalid JSON).
  - [ ] File input value reset after each load; `api.js` `exportGraph`/`importGraph` helpers.

### Acceptance Criteria
- [ ] Save → download named `<name>.em`; Load → round-trip restores nodes, positions, connections, and per-node configs exactly.
- [ ] Only `.em` files can be uploaded (client + server enforced); non-`.em` shows the themed error banner.
- [ ] Invalid `.em` (bad JSON, wrong format/version, unknown `typeId`, dangling edges) → 400 with descriptive message, canvas unchanged.
- [ ] Loading onto a non-empty canvas prompts replace-confirm; canvas clears and renders imported graph on confirm.
- [ ] `GraphControllerTest` green; modal is focus-trapped and `Esc`-closable; `prefers-reduced-motion` honored.

---

## Definition of Done (Project-Wide)

- [ ] Frontend uses **only** HTML/CSS/vanilla JS (SPEC §2.1) — zero framework/build tooling.
- [ ] Backend is Java 21 + Spring Boot + Maven (SPEC §2.2), single deployable JAR.
- [ ] All design tokens sourced from `theme.css` (SPEC §7.2, DESIGN.md §10).
- [ ] All API endpoints covered by tests.
- [ ] SPEC.md, DESIGN.md, ROADMAP.md reflect the shipped state.

---

## Backlog (Post-v1, unprioritized)

- WebSocket/SSE live telemetry push.
- Spring Security + user accounts.
- Persistence (Spring Data JPA, H2/PostgreSQL).
- Cloud save/load of named graphs (server-side persistence — distinct from the local `.em` files in M6).
- Mobile-responsive layout.
- Multi-region/edge-simulation presets.
