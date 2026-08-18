# EdgeMania.io

**Edge-computing simulation playground with a live security dashboard.**

Visually compose edge data pipelines (device → edge → cloud), run simulations, and monitor fleet telemetry — all wrapped in the *Analog Forge: Ink & Iron* manga-sketch aesthetic.

## Features

- **Node-graph editor** — Drag nodes from a palette, connect sockets with ink-wavy edges, configure properties in a side drawer.
- **Simulation engine** — Run pipelines for N ticks; results propagate across edges with per-node output values.
- **Security dashboard** — KPI tiles, hand-rolled SVG sparkline charts, and a severity-coded event feed (auto-refreshes every 5s).
- **Local save/load** — Export and import graphs as `.em` files (server-built, server-validated).

## Tech Stack

| Layer | Stack |
|-------|-------|
| Backend | Java 21, Spring Boot 3.3, Maven |
| Frontend | Vanilla HTML5, CSS3, ES6+ JavaScript — zero build step, zero frameworks |
| Fonts | Hanken Grotesk + JetBrains Mono (Google Fonts) |

The frontend lives in `src/main/resources/static` and is served by the same Spring Boot app — a single executable JAR.

## Getting Started

**Prerequisites:** Java 21+, Maven 3.9+

```bash
git clone https://github.com/arun/edgemania.io.git
cd edgemania.io
mvn clean spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

## Project Structure

```
edgemania.io/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/io/edgemania/       # Spring Boot backend
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── service/             # Business logic + in-memory store
│   │   │   ├── model/               # Domain models
│   │   │   ├── dto/                 # Request/response records
│   │   │   └── exception/           # Error handling
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/              # Frontend (served as-is)
│   │           ├── index.html       # Welcome page
│   │           ├── playground.html  # Node-graph editor
│   │           ├── dashboard.html   # Security dashboard
│   │           ├── css/             # theme.css, base.css, components.css
│   │           ├── js/              # api.js, graph.js, nodes.js, dashboard.js, theme.js
│   │           └── assets/brand/    # Logo SVG
│   └── test/java/io/edgemania/     # JUnit 5 + MockMvc tests
├── SPEC.md
├── DESIGN.md
└── ROADMAP.md
```

## Pages

| Page | URL | Description |
|------|-----|-------------|
| Welcome | `/` | Hero landing with feature highlights and CTAs |
| Playground | `/playground.html` | Visual node-graph editor with property drawer and simulation runner |
| Dashboard | `/dashboard.html` | Live KPI tiles, sparkline charts, and event feed |

## Running Tests

```bash
mvn test
```

## Related Documentation

- [SPEC.md](SPEC.md) — Full technical specification, API contracts, and data model
- [DESIGN.md](DESIGN.md) — Visual language, color tokens, typography, and component specs
- [ROADMAP.md](ROADMAP.md) — Milestone plan (M1–M6) with tasks and acceptance criteria
