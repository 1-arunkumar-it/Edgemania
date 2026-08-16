# EdgeMania.io — Design Specification

**Theme:** Analog Forge: Ink & Iron (Manga Sketch) · **Mode:** Dark · **Related:** [SPEC.md](SPEC.md), [ROADMAP.md](ROADMAP.md)

This document is the authoritative source for the visual language. Every value below maps 1:1 to CSS custom properties in `src/main/resources/static/css/theme.css`.

---

## 1. Brand & Style

A **Manga Sketch** aesthetic: high-performance technical utility rendered like a technical Seinen manga — a master engineer's field journal where every node and panel is hand-inked with obsessive detail.

| Principle | Meaning |
|---|---|
| **Inked Authenticity** | Every container/button looks hand-drawn: irregular line weights, sketchy corners, `±1px` stroke variance. |
| **Technical Grittiness** | Depth via cross-hatching (tonal shading), never gradients/blur. |
| **Structured Chaos** | Rigid professional grid, executed with "ink-on-paper" texture. |

- Reject sterile SaaS perfection. Text stays **perfectly rendered** (never skewed/hand-written); only containers are sketchy.
- Emotional response: tactile involvement — as if operating a physical manuscript of a complex machine.

---

## 2. Color Tokens

Derived from Gruvbox Dark, optimized for high-contrast ink-on-dark-paper.

| Token | Hex | Usage |
|---|---|---|
| `--ink-black` | `#0c0906` | All bold outlines, "drop stroke" shadows, hatch lines, text on primary |
| `--paper-off-white` | `#f2dfce` | Inverse/light surfaces, occasional ink-on-light accents |
| `--primary` | `#ffb68a` | Primary actions, focus, active states |
| `--primary-container` | `#fe8019` | Burnt-orange "highlighter ink"; primary buttons, critical alerts |
| `--on-primary` | `#522300` | Text on primary containers |
| `--on-primary-container` | `#5f2a00` | Text on primary-container |
| `--secondary` | `#cacd39` | Success states, completed sims, valid connections |
| `--secondary-container` | `#a1a401` | Secondary button fill, toggle ON |
| `--tertiary` | `#8aceff` | Info accents, selected node headers |
| `--tertiary-container` | `#00adf6` | Info fill (node headers, links) |
| `--surface` | `#1c110b` | Iron-deep canvas (page/panel background) |
| `--surface-dim` | `#1c110b` | App shell background |
| `--surface-bright` | `#44362e` | Highest panels |
| `--surface-container-lowest` | `#160c06` | Input wells, recessed fields |
| `--surface-container-low` | `#241912` | — |
| `--surface-container` | `#291d16` | Standard panel surface |
| `--surface-container-high` | `#342720` | Hover fills, raised controls |
| `--surface-container-highest` | `#3f322a` | Active rows, headers |
| `--surface-variant` | `#3f322a` | Surface variant |
| `--hatch-shade` | `#3e3327` | Cross-hatch tonal shade, panel header bars |
| `--on-surface` | `#f4ded3` | Primary text |
| `--on-surface-variant` | `#dfc0b0` | Secondary text |
| `--outline` | `#a68b7c` | Borders, dividers (mid ochre) |
| `--outline-variant` | `#574236` | Subtle dividers |
| `--error` | `#ffb4ab` | Errors, failed runs |
| `--error-container` | `#93000a` | Error fills |
| `--on-error` | `#690005` | Text on error fills |
| `--inverse-surface` | `#f4ded3` | Inverse (light) panels |
| `--inverse-on-surface` | `#3b2e26` | Text on inverse |

### Semantic mapping

- **Primary (burnt orange)** → critical actions, active execution, focus rings.
- **Secondary (green)** → success, completed, valid connections, toggles ON.
- **Tertiary (blue)** → info, selected headers, hyperlinks.
- **Error (red)** → failures, alerts, destructive actions.
- **Neutrals** → warm layered hierarchy; darkest for floors, ochre mid-tones for borders/inactive text.

---

## 3. Typography Tokens

Dual-font strategy: **Hanken Grotesk** (UI/navigation, heavy for titles) + **JetBrains Mono** (all technical data).

| Token | Font | Size | Weight | Line-Height | Letter-Spacing | Use |
|---|---|---|---|---|---|---|
| `--font-headline-lg` | Hanken Grotesk | 32px | 800 | 40px | -0.03em | Page/hero titles ("title card") |
| `--font-headline-md` | Hanken Grotesk | 24px | 700 | 32px | normal | Section titles |
| `--font-body-lg` | Hanken Grotesk | 16px | 400 | 24px | normal | Body copy |
| `--font-body-sm` | Hanken Grotesk | 14px | 400 | 20px | normal | Secondary copy |
| `--font-code-md` | JetBrains Mono | 14px | 400 | 20px | normal | Code, identifiers |
| `--font-label-caps` | JetBrains Mono | 11px | 700 | 16px | 0.1em | Panel/section headers, uppercase |
| `--font-data-tabular` | JetBrains Mono | 13px | 500 | 16px | normal | Numeric readouts, telemetry, inputs |

**Rules**
- All numeric readouts, node labels, property values, timestamps → JetBrains Mono (tabular, aligned columns).
- `label-caps` for every panel header bar and property-panel section title.
- `headline-lg` used sparingly (workspace/page titles only).
- Fallback stacks: `'Hanken Grotesk', system-ui, sans-serif` · `'JetBrains Mono', ui-monospace, monospace`.

---

## 4. Spacing & Shape Tokens

| Token | Value | Use |
|---|---|---|
| `--space-unit` | 4px | Base unit; all spacing is a multiple of 4px |
| `--space-gutter` | 12px | Between sibling controls |
| `--space-margin-sm` | 16px | Content within a panel |
| `--space-margin-md` | 24px | Major architectural breaks |
| `--ink-bleed` | 2px | Allowed stroke overshoot/overlap |
| `--radius` | 4px | Global corner radius (buttons, inputs, panels, nodes) |
| `--radius-sm` | 2px | Tiny elements |
| `--radius-full` | 9999px | Pills/toggles |

---

## 5. Elevation & Depth (Graphic Only)

No blur or drop-shadow lighting effects. Depth is purely graphic:

1. **Manual Hatching** — recessed surfaces (input wells, node-graph canvas) use a repeating 45° diagonal cross-hatch pattern via inline SVG/CSS:
   ```css
   --hatch: url("data:image/svg+xml,...") /* 8px stroke #3e3327 on transparent */
   ```
2. **Ink Stacking** — higher-hierarchy elements get thicker ink outlines: primary/active = 3px, background = 1px.
3. **Drop Stroke** — raised elements (buttons, cards) get a solid `ink-black` offset shape 3px behind them (2D comic pop-out), not a shadow:
   ```css
   box-shadow: 3px 3px 0 0 var(--ink-black);
   ```
   On hover the element shifts `1px` down/right toward its stroke; on `:active` it sits fully on it.

### Sketch borders

- No border is perfectly straight. Implement with:
  - An SVG `feTurbulence`-based border/backdrop for large panels, **or**
  - Multiple overlapping strokes + `border-radius` wobble; **and**
  - **Corner over-draw:** strokes overshoot corners by 1–2px (ink pen didn't lift in time).
- Input borders: 1px, "broken" in 1–2 spots (ink-starved pen).
- Focused elements: border weight increases + wobble becomes more pronounced; focus ring = 2px `--primary`.

---

## 6. Component Specs

### 6.1 Buttons

- 2px bold ink border, 3px `ink-black` drop stroke, `4px` radius.
- **Primary:** fill `--primary-container` (#fe8019), text `ink-black`, weight 700/800.
- **Secondary:** fill `--surface-container-high`, text `--on-surface`, border `--outline`.
- **Ghost/tertiary:** transparent fill, 1px border.
- **States:** hover → translate 1px down/right; active → fully onto stroke; disabled → 40% opacity, no stroke shift.
- Text: `--font-body-lg` (buttons) or `--font-label-caps` (toolbar buttons).

### 6.2 Input Fields

- **Inset look:** interior top/left edges hatched (cross-hatch pattern); background `--surface-container-lowest`.
- Border: 1px sketchy `--outline`, slightly broken.
- Value text: `--font-data-tabular`, color `--on-surface`.
- **Draggable numeric inputs:** horizontal-resize cursor; background fill tracks value % (`--primary-container` at low opacity).
- Focus: 2px `--primary` outline, wobble increases.

### 6.3 Cards & Panels (Manga Panels)

- Large containers: heavy **3px `ink-black` border**, `4px` radius.
- Panel header bar: dedicated top bar, fill `--hatch-shade`, title in `--font-label-caps`.
- Content: `--space-margin-sm` padding; separated by 1px `--outline-variant` dividers.
- Background: `--surface-container` (standard) or `--surface-bright` (highlighted).

### 6.4 Node Graph

- **Nodes:** rectangular, 4px radius, 2px ink border; header = "scribbled" fill in the node's category color.
- **Sockets:** **hex shapes** (not circles), bold ink outline, color-coded by type:
  - orange `--primary-container` = boolean/control
  - purple-ish tertiary `--tertiary` = data/vector
  - green `--secondary` = output
- **Connection lines:** bold solid ink lines with slight hand-drawn **waviness** (SVG cubic-bezier with jitter, or two overlapping strokes).

### 6.5 Checkboxes & Radios

- **Checkbox:** an "X" drawn with two quick pen strokes when checked; 2px ink box otherwise.
- **Radio:** filled with a messy **ink spiral** when selected; hollow circle otherwise.

### 6.6 Toggle Switches

- Small rectangular pills; ON → `--secondary-container` fill, OFF → border `--outline-variant`.

### 6.7 Charts (Dashboard)

- Hand-rolled SVG `<polyline>`/`<path>`, stroke `--primary` (or category color), fill hatch pattern.
- Grid lines: 1px `--outline-variant` at 8px spacing.
- Labels: `--font-data-tabular`.

### 6.8 Modals & File Dialogs

- **Modal shell:** dim overlay (`rgba(0,0,0,0.6)`) with a centered manga panel — 3px `ink-black` border, `--surface-container` fill, `label-caps` header bar, 4px radius, 3px drop stroke.
- Focus-trapped; close via `Esc` or a ✕ button; `prefers-reduced-motion` disables the entrance animation.
- **Save naming modal (`.em` export):** single text input (`--font-data-tabular`), hint showing the resulting filename as `<name>.em` in JetBrains Mono, Cancel / Save buttons (Save = `--primary-container`).
- **Replace-confirm modal:** used by Load when the canvas is non-empty — "Replace current graph?" with Cancel / Replace (Replace = `--primary-container`).
- **File inputs (`.em` upload):** hidden native `<input type="file" accept=".em">`; the visible "Load" control is a standard button. Invalid file → themed error banner (no native file-error UI).

---

## 7. Page Layouts

Global shell: sticky top nav (logo mark left; links Playground / Dashboard; "Launch" CTA right). Nav uses `label-caps` links, active link = `--primary-container` underline or 3px offset stroke.

### 7.1 Welcome (`index.html`)

```
┌────────────────────────────────────────────────────────┐
│ NAV: [logo] Playground Dashboard            [Launch ▸] │
├────────────────────────────────────────────────────────┤
│                    HERO (headline-lg)                  │
│              subcopy (body-lg, max 60ch)               │
│         [Run a Simulation ▸]  [View Dashboard]         │
│                 (manga panel hero card)                │
├────────────────────────────────────────────────────────┤
│  ┌─panel──┐   ┌─panel──┐   ┌─panel──┐   ┌─panel──┐    │
│  │Feature │   │Feature │   │Feature │   │Feature │    │
│  │ 1      │   │ 2      │   │ 3      │   │ 4      │    │
│  └────────┘   └────────┘   └────────┘   └────────┘    │
├────────────────────────────────────────────────────────┤
│                        FOOTER                          │
└────────────────────────────────────────────────────────┘
```
- Hero bg: faint hatch texture + a large decorative node-graph SVG.
- Feature cards: 3px manga panels; icon in colored node-header style.

### 7.2 Playground (`playground.html`)

```
┌──────────────────────────────────────────────────────────────┐
│ NAV (same)                                                   │
├──────────┬───────────────────────────────────┬───────────────┤
│ PALETTE  │        NODE CANVAS (hatched well) │  PROPERTIES   │
│ label-   │  [n1]───[n2]                      │  label-caps   │
│ caps     │      ╲    ╲                       │  header bar   │
│ [device] │       ╲    [n3]─[cloud]           │  …fields…     │
│ [edge]   │  (wavy ink edges, hex sockets)    │  status footer│
│ [cloud]  │                                  │               │
├──────────┴───────────────────────────────────┴───────────────┤
│ TOOLBAR: [New] [Sample] [Save ▾] [Load] [Run ▸]  snap ON st   │
└──────────────────────────────────────────────────────────────┘
```
- Palette: node type list, each with category-color swatch; draggable into canvas.
- Canvas: `--surface-container-lowest` + hatch; nodes snap to 4px grid.
- Property drawer (Node Configuration): 320–360px; header bar = node category color; fields per type; "Save" = `--primary-container` button; run status footer.
- **Toolbar Save ▾** opens the naming modal (§6.8) → downloads `<name>.em` via `POST /api/graphs/export`. **Toolbar Load** opens the hidden `.em` file input → uploads via `POST /api/graphs/import`; non-empty canvas prompts the replace-confirm modal. See SPEC §5.6.

### 7.3 Security Dashboard (`dashboard.html`)

```
┌──────────────────────────────────────────────────────────────┐
│ NAV (same)                                                   │
├──────────────────────────────────────────────────────────────┤
│ KPI TILES: [CPU %] [MEM %] [P95 LAT] [NODES] [RUNS] [EVENTS]│
│            (6 manga panel tiles, data-tabular values)        │
├──────────────────────────────────┬───────────────────────────┤
│        CHART PANEL (SVG area)    │   EVENT FEED             │
│  CPU / Memory / Latency history  │   header bar label-caps  │
│  hatch fill + ink grid lines     │   severity-coded rows    │
│                                  │   · 04:12:55 info …      │
│                                  │   ✕ 04:12:49 error …     │
├──────────────────────────────────┴───────────────────────────┤
│  footer: last refresh time (mono) · polling 5s badge         │
└──────────────────────────────────────────────────────────────┘
```
- KPI tiles: 3px manga panel, label `label-caps` top, value `data-tabular` 28–32px.
- Event feed: severity dot + time (mono) + message; severity colors map to §2 semantic mapping.
- Auto-refresh 5s; pause when tab hidden.

---

## 8. Motion

- Keep it minimal and physical (press/ink feel):
  - Buttons/selectable nodes: 80–120ms transform on hover/active.
  - Drawer slide: 150ms ease-out.
  - Sparkline draws: 400ms stroke-dashoffset transition.
- **`prefers-reduced-motion: reduce`** → disable all transforms/transitions; no animation.

---

## 9. Accessibility

- Text on primary-container uses `ink-black` (AA+); `on-surface` on `surface` ≈ 13:1.
- Focus visible everywhere (2px `--primary` ink ring + wobble increase).
- All interactive elements keyboard-reachable (tab order, `Delete` shortcut documented, `aria-label` on icon-only controls).
- Semantic HTML: `<header>`, `<main>`, `<nav>`, `<button>`, proper `<label for>`.
- Charts: provide textual fallback (value tables or `aria-label` with current values).

---

## 10. Implementation Notes

- All of the above → CSS custom properties in `theme.css`; components reference variables only (no stray hex).
- Hatch pattern, wavy edges, hex sockets, drop strokes → inline SVG data-URIs / SVG in `assets/`.
- Document any token addition in this file + `theme.css` together.
