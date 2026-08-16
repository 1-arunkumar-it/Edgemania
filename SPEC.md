# EdgeMania.io — Technical Specification

**Status:** Draft v1 · **Owner:** Engineering · **Related docs:** [DESIGN.md](DESIGN.md) (theme/layout), [ROADMAP.md](ROADMAP.md) (milestones)

---

## 1. Project Overview

EdgeMania.io is an **edge-computing simulation playground** bundled with a **live security/ops dashboard**. Users design node-based data-flow simulations (sensors → processors → outputs), run them, and monitor simulated edge-network telemetry in real time.

### 1.1 Product Goals

- Let users visually compose edge-data pipelines with a draggable node graph.
- Provide instant feedback through a responsive property panel that edits the selected node.
- Surface live "edge fleet" telemetry (CPU, memory, latency, events) on a dashboard.
- Deliver a distinctive, cohesive brand experience: the **Analog Forge: Ink & Iron** manga-sketch aesthetic (see DESIGN.md).

### 1.2 Target Users (v1 Personas)

| Persona | Need |
|---|---|
| EdgeOps Engineer | Quickly prototype edge pipelines and inspect per-node config. |
| Security Analyst | Watch fleet health and event streams without leaving the console. |
| Hobbyist / Demo user | Explore the product from the welcome page with zero friction. |

### 1.3 In-Scope Pages (v1)

1. **Welcome** — `index.html` — landing/hero, feature highlights, CTA.
2. **Playground** — `playground.html` — node-graph editor canvas.
3. **Node Configuration** — integrated into the Playground as a property drawer/panel (no separate route in v1).
4. **Security Dashboard** — `dashboard.html` — live metric tiles, sparkline charts, event feed.

### 1.4 Non-Goals (v1)

- No user accounts / authentication / authorization.
- No database persistence (in-memory store only; data resets on restart).
- No deployment/CI pipeline, no Docker images.
- No real edge-device communication — all telemetry is simulated server-side.
- No mobile-specific UI (desktop-first; should not break below 1024px).

---

## 2. Tech Stack (Strict Constraints)

### 2.1 Frontend — `src/main/resources/static`

| Allowed | Forbidden |
|---|---|
| HTML5 (semantic elements) | React, Vue, Angular, Svelte, HTMX |
| CSS3 (custom properties, flexbox, grid, animations) | Any CSS framework (Tailwind, Bootstrap) |
| Vanilla JavaScript (ES6+ modules) | Any JS framework/library (jQuery, D3, Chart.js, Lodash) |
| Inline SVG (icons, textures, charts) | CSS/JS preprocessors (Sass, Less) |
| Google Fonts `<link>` (Hanken Grotesk, JetBrains Mono) | npm / yarn / build tooling / bundlers |

- **Zero build step.** Files are served as-is from the static directory.
- JS is authored as ES modules; pages include a single module entry point (`<script type="module">`).
- No third-party runtime assets are downloaded by the app other than the two Google Fonts.

### 2.2 Backend — Spring Boot

- **Java 21** (record types, pattern matching, sealed interfaces where useful).
- **Spring Boot 3.3.x**, packaged via **Maven**.
- Dependencies:
  - `spring-boot-starter-web` (REST + static resources + JSON via Jackson)
  - `spring-boot-starter-validation` (Bean Validation on request DTOs)
  - `spring-boot-starter-test` (JUnit 5, MockMvc, AssertJ)
- No Spring Security, no Spring Data / JPA, no persistence in v1.

### 2.3 Serving Model

- Frontend lives in `src/main/resources/static` and is served by the same Spring Boot app → a single executable JAR.
- API namespace is `/api/**`; everything else falls through to static resources.
- No CORS configuration required (same-origin only).

---

## 3. Information Architecture & Frontend Architecture

### 3.1 Multi-Page Application (MPA)

- One HTML file per page; shared assets in `static/css` and `static/js`.
- Cross-page navigation via ordinary `<a>` links (no client-side router).
- Shared pieces:
  - `static/css/theme.css` — design tokens (variables only).
  - `static/css/base.css` — reset, typography, layout primitives.
  - `static/css/components.css` — button/input/panel/node/chart styles.
  - `static/js/api.js` — thin `fetch` wrapper for `/api/**`.
  - `static/js/theme.js` — runtime helpers (e.g., prefers-reduced-motion, hatch canvas generator).

### 3.2 Page: Welcome (`index.html`)

- Top nav (logo, links to Playground/Dashboard, "Launch" CTA).
- Hero: headline, subcopy, primary CTA to Playground, secondary link to Dashboard.
- Feature section: 3–4 manga-panel cards describing capabilities.
- Footer.
- Minimal JS (nav state, no API calls required).

### 3.3 Page: Playground (`playground.html`)

- **Layout zones:** top toolbar · left node palette · center canvas (hatched "well") · right property drawer.
- Node palette lists node types from `GET /api/nodes` (with type header color swatches).
- Canvas behaviors (all vanilla JS):
  - Drag node from palette → drop to add node (positioned at drop point).
  - Drag nodes to move; grid snapping at 4px multiples.
  - Connect output socket → input socket by dragging a connection line (wavy bezier path).
  - Click node → select → property drawer loads node config.
  - Context/shortcut: delete selected node/edge (`Delete` key).
  - Toolbar: New Graph, Load Sample, Run (→ `POST /api/simulations/run`), layout actions.
  - Local save/load: **Save** opens a naming modal then downloads a `.em` file (`POST /api/graphs/export`); **Load** uploads a `.em` file (`POST /api/graphs/import`) and replaces the canvas (themed confirm if nodes already exist). See §5.6.
- **Node Configuration drawer** (right panel):
  - Header (node id + category color bar), fields per node type (from catalog schema).
  - Fields: text inputs, numeric draggable sliders (value fill tracks percentage), toggles, selects.
  - Save via `PUT /api/nodes/{id}`; values render as `data-tabular` (JetBrains Mono).
  - Run results/status footer when a simulation completes.

### 3.4 Page: Security Dashboard (`dashboard.html`)

- **Layout zones:** top nav · KPI tile row · chart area · event feed.
- KPI tiles (from `GET /api/dashboard/metrics`): Active Nodes, Active Simulations, CPU %, Memory %, P95 Latency, Events (24h).
- Charts: hand-rolled SVG sparkline/area charts from `/api/dashboard/metrics/history` (no chart library).
  - Rendered with inline SVG `<polyline>`/`<path>`; axis labels in `data-tabular`.
- Event feed: newest-first list of `{time, severity, message}`; severity color-coded (primary orange / secondary green / tertiary blue / error red).
- **Live refresh:** poll metrics every 5s with `setInterval` + `fetch` (no WebSocket in v1). Pause when tab hidden (`visibilitychange`).

---

## 4. Backend Architecture

### 4.1 Package Layout

```
io.edgemania
├── EdgeManiaApplication.java
├── config/
│   └── WebConfig.java                 # static fallback + CORS-safe defaults
├── controller/
│   ├── NodeCatalogController.java
│   ├── SimulationController.java
│   ├── DashboardController.java
│   └── GraphController.java
├── service/
│   ├── NodeCatalogService.java
│   ├── SimulationService.java
│   ├── DashboardService.java
│   └── GraphFileService.java
├── model/
│   ├── Node.java
│   ├── NodeType.java
│   ├── Edge.java
│   ├── Graph.java
│   ├── Simulation.java
│   ├── SimulationRun.java
│   ├── MetricSnapshot.java
│   └── MetricPoint.java
├── dto/   (record request/response DTOs)
└── exception/
    ├── ApiException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

### 4.2 Architecture Rules

- **Controller layer:** thin — maps DTOs to models, delegates to services, returns DTOs. Never contains business logic.
- **Service layer:** all business logic; holds the in-memory store (`ConcurrentHashMap` keyed by UUID) with `synchronized`/`ConcurrentHashMap` safety.
- **Models** are immutable where possible (Java records for value types; plain classes for mutable nodes).
- **Error handling:** all thrown `ApiException`s and validation failures are mapped by `GlobalExceptionHandler` (`@RestControllerAdvice`) to a consistent JSON envelope (see §5.5).
- **IDs:** `UUID.randomUUID().toString()`; stable fake IDs may be baked into sample data.
- **Deterministic simulation:** the simulation engine runs a fixed number of ticks, propagating values across edges; results stored on the run.

### 4.3 In-Memory Store (v1)

- `ConcurrentHashMap<UUID, Node>` and `ConcurrentHashMap<UUID, SimulationRun>` inside services.
- Seeded sample data at startup: ~8 catalog node types and a sample graph (see `SampleData` helper).
- No repository abstraction needed.

---

## 5. API Contracts

Base URL: `/api` · Content-Type: `application/json` · Errors: envelope in §5.5.

### 5.1 Node Catalog

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/nodes` | List all node types in the palette |
| GET | `/api/nodes/{id}` | Single node instance detail |
| POST | `/api/nodes` | Create a node instance |
| PUT | `/api/nodes/{id}` | Update node instance properties |
| DELETE | `/api/nodes/{id}` | Remove a node instance |

**`GET /api/nodes` → 200**

```json
{
  "types": [
    {
      "id": "sensor",
      "label": "Sensor",
      "category": "source",
      "color": "secondary",
      "sockets": {
        "inputs": [],
        "outputs": ["data"]
      },
      "properties": [
        { "key": "frequency", "label": "Frequency", "type": "number", "min": 0.1, "max": 60, "step": 0.1, "default": 1.0 }
      ]
    },
    {
      "id": "filter",
      "label": "Filter",
      "category": "logic",
      "color": "tertiary",
      "sockets": { "inputs": ["data"], "outputs": ["data"] },
      "properties": [
        { "key": "threshold", "label": "Threshold", "type": "number", "min": 0, "max": 100, "step": 0.5, "default": 50.0 }
      ]
    }
  ],
  "instances": []
}
```

**`POST /api/nodes` — request**

```json
{
  "typeId": "sensor",
  "label": "sensor-01",
  "x": 480,
  "y": 320,
  "properties": { "frequency": 1.0 }
}
```

**response → 201** (same shape as GET `/api/nodes/{id}` below)

```json
{
  "id": "3f2c…",
  "typeId": "sensor",
  "label": "sensor-01",
  "category": "source",
  "x": 480,
  "y": 320,
  "properties": { "frequency": 1.0 },
  "status": "idle"
}
```

### 5.2 Simulations

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/simulations` | List past runs (id, label, status, finishedAt) |
| POST | `/api/simulations/run` | Run a graph for N ticks |
| GET | `/api/simulations/{runId}` | Run detail incl. per-node step values |

**`POST /api/simulations/run` — request**

```json
{
  "graph": {
    "nodes": [
      { "id": "n1", "typeId": "sensor", "label": "sensor-01", "properties": { "frequency": 1.0 } },
      { "id": "n2", "typeId": "filter", "label": "filter-01", "properties": { "threshold": 50.0 } }
    ],
    "edges": [
      { "from": "n1", "fromSocket": "data", "to": "n2", "toSocket": "data" }
    ]
  },
  "ticks": 100,
  "tickMs": 10
}
```

**response → 202**

```json
{ "runId": "9c1e…", "status": "RUNNING", "ticks": 100, "startedAt": "2026-08-16T04:12:55Z" }
```

**`GET /api/simulations/{runId}` → 200**

```json
{
  "runId": "9c1e…",
  "status": "COMPLETED",
  "ticks": 100,
  "startedAt": "2026-08-16T04:12:55Z",
  "finishedAt": "2026-08-16T04:12:56Z",
  "nodeOutputs": [
    { "nodeId": "n1", "label": "sensor-01", "lastValue": 72.4 },
    { "nodeId": "n2", "label": "filter-01", "lastValue": 50.0 }
  ]
}
```

### 5.3 Dashboard

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/dashboard/metrics` | Current fleet snapshot |
| GET | `/api/dashboard/metrics/history?window=5m` | Time series for charts |

**`GET /api/dashboard/metrics` → 200**

```json
{
  "nodes": 14,
  "simulations": 3,
  "cpu": { "value": 42.3, "unit": "%" },
  "memory": { "value": 68.1, "unit": "%" },
  "latencyP95": { "value": 18.4, "unit": "ms" },
  "events24h": 217,
  "events": [
    { "id": "e1", "time": "04:12:55", "severity": "info", "message": "Pipeline edge-sim-07 completed" }
  ]
}
```

**`GET /api/dashboard/metrics/history?window=5m` → 200**

```json
{
  "window": "5m",
  "points": [
    { "t": 1755300000000, "cpu": 41.2, "memory": 67.8, "latency": 17.9 }
  ]
}
```

### 5.4 Validation

- Request DTOs annotated with Bean Validation: `@NotBlank` on required strings, `@NotNull` on required numbers, `@Min/@Max` on ranges, `@Size` on collections.
- `ticks` clamped to `1..10_000`, `tickMs` to `1..1_000`.
- Graph must reference only existing node ids; duplicate edge ids rejected (409).

### 5.5 Error Envelope

All non-2xx responses:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Node 3f2c… not found",
  "path": "/api/nodes/3f2c…",
  "timestamp": "2026-08-16T04:12:55Z"
}
```

| Code | When |
|---|---|
| 400 | Validation failure (`message` lists first constraint); invalid `.em` file (§5.6) |
| 404 | Unknown id / resource |
| 409 | Conflict (duplicate id, invalid graph edge) |
| 500 | Unexpected error (logged, generic message returned) |

### 5.6 Graph Files (.em)

Local save/load of Playground graphs as custom **`.em` (EdgeMania)** files — entirely user-local, no cloud storage. The backend is the single authority for **building and parsing** `.em` files (via Spring's auto-configured Jackson `ObjectMapper`); the frontend never constructs or parses the file itself.

**`.em` file format** (`Content-Type: application/x-edgemania`):

```json
{
  "format": "edgemania",
  "version": 1,
  "name": "my-pipeline",
  "savedAt": "2026-08-16T04:12:55Z",
  "graph": {
    "nodes": [
      { "id": "n1", "typeId": "sensor", "label": "sensor-01", "x": 480, "y": 320, "properties": { "frequency": 1.0 } }
    ],
    "edges": [
      { "id": "e1", "from": "n1", "fromSocket": "data", "to": "n2", "toSocket": "data" }
    ]
  }
}
```

- The `format`/`version` envelope guards against future schema changes; `version` must equal `1`.
- `graph.nodes` and `graph.edges` reuse the models defined in §6 (node instances carry `id`, `typeId`, `label`, `x`, `y`, `properties`; edges carry `id`, `from`, `fromSocket`, `to`, `toSocket`).

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/graphs/export` | Build a `.em` file from the current graph and return it as a download |
| POST | `/api/graphs/import` | Parse an uploaded `.em` file and return its graph for rendering |

**`POST /api/graphs/export` — request** (`application/json`)

```json
{
  "name": "my-pipeline",
  "graph": { "nodes": [...], "edges": [...] }
}
```

**response → 200** — `Content-Type: application/x-edgemania`, `Content-Disposition: attachment; filename="my-pipeline.em"`, body is the canonical `.em` JSON above. The `name` is sanitized server-side (only `[A-Za-z0-9 _-]`, max 64 chars, path separators stripped) and given a `.em` extension. The frontend also sets `a.download` so the user's chosen name always wins.

**`POST /api/graphs/import` — request** (`multipart/form-data`, field `file`)

Accepts a `.em` upload. Response → 200:

```json
{
  "name": "my-pipeline",
  "savedAt": "2026-08-16T04:12:55Z",
  "graph": { "nodes": [...], "edges": [...] }
}
```

**Import validation rules (all → 400 with a descriptive `message`):**
- File extension must be `.em` (case-insensitive) — non-`.em` files are rejected.
- Body parses as JSON via Jackson; must declare `format: "edgemania"` and `version: 1`.
- `graph.nodes` must be present and well-formed; duplicate node/edge ids rejected.
- Every edge must reference existing node ids and valid socket names on those node types.
- Every `typeId` must exist in the node catalog — unknown types are rejected, with the missing type named in `message`.

**Config:** `application.properties` — `spring.servlet.multipart.max-file-size=5MB`, `spring.servlet.multipart.max-request-size=6MB`.

---

## 6. Data Model

### NodeType
`id`, `label`, `category` (`source | process | logic | output`), `color` (theme token name), `sockets` (`inputs[]`, `outputs[]` of socket names), `properties[]` (schema: `key, label, type, min, max, step, default, options?`).

### Node
`id`, `typeId`, `label`, `category`, `x`, `y`, `properties` (map of current values), `status` (`idle | running | complete | error`).

### Edge
`id`, `from` (nodeId), `fromSocket`, `to` (nodeId), `toSocket`.

### Graph
`nodes[]`, `edges[]`.

### SimulationRun
`id`, `label`, `status` (`RUNNING | COMPLETED | FAILED`), `ticks`, `startedAt`, `finishedAt`, `nodeOutputs[]`.

### MetricSnapshot / MetricPoint
Snapshot: `nodes`, `simulations`, `cpu`, `memory`, `latencyP95`, `events24h`, `events[]`. Point: `t`, `cpu`, `memory`, `latency`.

---

## 7. Project Structure & Conventions

### 7.1 Directory Tree

```
edgemania.io/
├── SPEC.md
├── DESIGN.md
├── ROADMAP.md
├── .gitignore
├── pom.xml
└── src/
    ├── main/
    │   ├── java/io/edgemania/          # packages per §4.1
    │   └── resources/
    │       ├── application.properties
    │       └── static/
    │           ├── index.html          # Welcome
    │           ├── playground.html     # Playground + Node Config drawer
    │           ├── dashboard.html      # Security Dashboard
    │           ├── css/
    │           │   ├── theme.css       # tokens (from DESIGN.md)
    │           │   ├── base.css
    │           │   └── components.css
    │           ├── js/
    │           │   ├── api.js
    │           │   ├── theme.js
    │           │   ├── graph.js
    │           │   ├── nodes.js
    │           │   └── dashboard.js
    │           └── assets/
    │               └── brand/          # logo mark (SVG)
    └── test/java/io/edgemania/
        ├── controller/                 # MockMvc tests
        └── service/                    # unit tests
```

### 7.2 Conventions

- **CSS:** all design decisions as custom properties in `theme.css`; components only reference variables. No hardcoded hex in components.
- **Naming:** kebab-case files; BEM-ish class names (`.btn`, `.btn--primary`, `.node-graph__canvas`).
- **JS:** ES modules; one default-export object per module; no global scope pollution (each page imports its entry module).
- **Java:** records for DTOs; services named `<X>Service`; controllers named `<X>Controller`; `final` on constructor params; dependency injection via constructor.
- **JSON:** `camelCase` keys; timestamps as ISO-8601 UTC.
- **Fonts:** Google Fonts loaded once in a shared `<head>` include; fallback stacks: `'Hanken Grotesk', system-ui, sans-serif` and `'JetBrains Mono', ui-monospace, monospace`.

---

## 8. Build, Run & Test

### 8.1 Commands

```bash
# Build & run (from repo root)
mvn clean spring-boot:run          # http://localhost:8080

# Tests
mvn test

# Package
mvn clean package                  # target/edgemania-0.1.0.jar
```

### 8.2 Backend Tests (JUnit 5 + MockMvc)

- `NodeCatalogControllerTest` — GET list, POST create (201), POST invalid body (400), DELETE (204).
- `SimulationControllerTest` — run happy path (202), unknown node id in graph (409), ticks out of range (400).
- `DashboardControllerTest` — metrics shape, history window param.
- `GraphControllerTest` — export happy path (200, correct `Content-Disposition` filename + `application/x-edgemania` content-type), invalid name (400); import valid `.em` round-trip (200), wrong extension (400), malformed JSON (400), wrong format/version (400), duplicate ids (400), edge to missing node (400), unknown `typeId` (400), oversized file (400).
- `GlobalExceptionHandlerTest` — envelope shape for 404/400.

### 8.3 Frontend Manual Checklist

- [ ] Welcome renders with manga panels + hatched surfaces; links work.
- [ ] Playground: drag from palette, move node, connect sockets, select → drawer edits + saves.
- [ ] Playground save/load: Save opens naming modal → downloads `<name>.em`; Load accepts `.em` only; non-`.em` rejected with a themed error banner; Save → Load round-trip preserves nodes, positions, connections, and per-node config (replace canvas after confirm).
- [ ] Drawer numeric fields drag-adjust with value-fill background.
- [ ] Run simulation → status footer updates; `Delete` removes selection.
- [ ] Dashboard: tiles render, sparklines draw, feed updates every 5s; values in JetBrains Mono.
- [ ] Tab hidden → polling pauses; `prefers-reduced-motion` honored.
- [ ] No console errors; no network calls outside `/api` and fonts.

### 8.4 Definition of Done

- Code matches §2 constraints (no frameworks, no build step).
- All tokens pulled from `theme.css` (no stray hex in components).
- All `/api` endpoints tested green via `mvn test`.
- Manual checklist above passes.
- Files formatted and no dead code; SPEC/DESIGN/ROADMAP kept in sync.

---

## 9. Milestones

See [ROADMAP.md](ROADMAP.md) for the milestone plan (M1–M6) with tasks and acceptance criteria.
