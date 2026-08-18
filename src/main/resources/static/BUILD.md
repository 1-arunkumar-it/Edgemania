# EdgeMania.io — Pipeline Builder Reference

**Purpose:** This document fully specifies the EdgeMania `.em` file format, node catalog, and validation rules so that an AI model can generate valid pipeline configurations. Users can ask any LLM to produce a `.em` file from this spec, then import it into the EdgeMania Playground.

---

## How It Works

1. **You** describe the edge-computing pipeline you want (topology, node properties, etc.).
2. **An AI** reads this document and generates a valid `.em` JSON file.
3. **You** save the output as a `.em` file and import it into EdgeMania via the **Load** button.

The Playground validates the file on import. If anything is wrong, it shows a descriptive error — fix the file and re-import.

---

## Node Type Catalog

EdgeMania has exactly **three** node types. Every node in a pipeline must use one of these `typeId` values.

### `device` — Data Source

| Field | Value |
|---|---|
| typeId | `"device"` |
| Label | Device |
| Category | source |
| Color | secondary |
| Output sockets | `["data"]` |
| Input sockets | `[]` (none) |

A `device` generates simulated telemetry data and sends it out through its `data` output socket. It has no inputs — it is always the starting point of a pipeline.

**Properties:**

| Key | Label | Type | Min | Max | Step | Default | Options |
|---|---|---|---|---|---|---|---|
| `device_type` | Device Type | select | — | — | — | `"camera"` | `"camera"`, `"gateway"`, `"thermostat"` |
| `data_rate` | Data Rate | number | 1 | 100 | 1 | 50 | — |

- `device_type` selects the kind of sensor/device. All three options are valid.
- `data_rate` is a throughput value in arbitrary units (simulated).

---

### `edge` — Edge Processor

| Field | Value |
|---|---|
| typeId | `"edge"` |
| Label | Edge |
| Category | process |
| Color | tertiary |
| Output sockets | `["data"]` |
| Input sockets | `["data"]` |

An `edge` node processes incoming data from its `data` input socket and forwards it through its `data` output socket. It sits between a `device` and a `cloud` in the pipeline.

**Properties:**

| Key | Label | Type | Min | Max | Step | Default | Options |
|---|---|---|---|---|---|---|---|
| `cpu_cores` | CPU Cores | number | 1 | 64 | 1 | 4 | — |
| `ram_gb` | RAM (GB) | number | 1 | 256 | 1 | 16 | — |

---

### `cloud` — Cloud Aggregator

| Field | Value |
|---|---|
| typeId | `"cloud"` |
| Label | Cloud |
| Category | output |
| Color | primary |
| Output sockets | `[]` (none) |
| Input sockets | `["data"]` |

A `cloud` node is the endpoint — it receives data from its `data` input socket and does not forward it. It represents a cloud server or analytics endpoint.

**Properties:**

| Key | Label | Type | Min | Max | Step | Default | Options |
|---|---|---|---|---|---|---|---|
| `latency_ms` | Latency (ms) | number | 1 | 500 | 1 | 45 | — |

---

## `.em` File Format

The file is **JSON** with the following envelope structure. The file extension must be `.em`.

```json
{
  "format": "edgemania",
  "version": 1,
  "name": "my-pipeline",
  "savedAt": "2026-08-18T12:00:00Z",
  "graph": {
    "nodes": [ ... ],
    "edges": [ ... ]
  }
}
```

### Envelope Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `format` | string | Yes | Must be exactly `"edgemania"` |
| `version` | integer | Yes | Must be exactly `1` |
| `name` | string | Yes | Pipeline name. Only `[A-Za-z0-9 _-]` allowed, max 64 characters |
| `savedAt` | string | Yes | ISO-8601 UTC timestamp, e.g. `"2026-08-18T12:00:00Z"` |
| `graph` | object | Yes | Contains `nodes` and `edges` arrays |

### Node Object

```json
{
  "id": "n1",
  "typeId": "device",
  "label": "cam-01",
  "x": 80,
  "y": 120,
  "properties": {
    "device_type": "camera",
    "data_rate": 80
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | Yes | Unique identifier. Convention: `"n1"`, `"n2"`, etc. |
| `typeId` | string | Yes | One of: `"device"`, `"edge"`, `"cloud"` |
| `label` | string | Yes | Display name shown on the node |
| `x` | number | Yes | Horizontal position on canvas (pixels) |
| `y` | number | Yes | Vertical position on canvas (pixels) |
| `properties` | object | Yes | Key-value map of the node's properties (see catalog above) |

**Positioning tips:**
- Use a left-to-right layout: devices at x=80, edges at x=360, clouds at x=620
- Space nodes vertically by ~100–140px when placing multiple nodes in a column
- The canvas origin (0,0) is top-left

### Edge Object

```json
{
  "id": "e1",
  "from": "n1",
  "fromSocket": "data",
  "to": "n2",
  "toSocket": "data"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | Yes | Unique identifier. Convention: `"e1"`, `"e2"`, etc. |
| `from` | string | Yes | ID of the source node |
| `fromSocket` | string | Yes | Output socket name on the source node (must be in that node type's `outputs`) |
| `to` | string | Yes | ID of the target node |
| `toSocket` | string | Yes | Input socket name on the target node (must be in that node type's `inputs`) |

---

## Validation Rules

The server validates every import. All of these must pass:

1. **Envelope:** `format` must be `"edgemania"`, `version` must be `1`.
2. **Node IDs unique:** No two nodes can share the same `id`.
3. **Edge IDs unique:** No two edges can share the same `id`.
4. **Edge references valid nodes:** Every edge's `from` and `to` must match an existing node `id`.
5. **Socket compatibility:** `fromSocket` must be in the source node type's output sockets. `toSocket` must be in the target node type's input sockets.
6. **Type IDs valid:** Every node's `typeId` must be one of `"device"`, `"edge"`, `"cloud"`.
7. **No self-loops:** An edge cannot have `from` equal to `to`.
8. **File extension:** The uploaded file must have a `.em` extension.

If any rule fails, the import returns a `400 Bad Request` with a descriptive message.

---

## Examples

### Example 1 — Simple Pipeline (3 nodes)

One camera → one edge processor → one cloud server.

```json
{
  "format": "edgemania",
  "version": 1,
  "name": "simple-pipeline",
  "savedAt": "2026-08-18T12:00:00Z",
  "graph": {
    "nodes": [
      {
        "id": "n1",
        "typeId": "device",
        "label": "cam-01",
        "x": 80,
        "y": 120,
        "properties": { "device_type": "camera", "data_rate": 80 }
      },
      {
        "id": "n2",
        "typeId": "edge",
        "label": "edge-01",
        "x": 360,
        "y": 120,
        "properties": { "cpu_cores": 4, "ram_gb": 16 }
      },
      {
        "id": "n3",
        "typeId": "cloud",
        "label": "cloud-01",
        "x": 640,
        "y": 120,
        "properties": { "latency_ms": 45 }
      }
    ],
    "edges": [
      { "id": "e1", "from": "n1", "fromSocket": "data", "to": "n2", "toSocket": "data" },
      { "id": "e2", "from": "n2", "fromSocket": "data", "to": "n3", "toSocket": "data" }
    ]
  }
}
```

### Example 2 — Multi-Device Converge (5 nodes)

Two cameras feeding into one edge processor, then to cloud.

```json
{
  "format": "edgemania",
  "version": 1,
  "name": "dual-camera-pipeline",
  "savedAt": "2026-08-18T12:00:00Z",
  "graph": {
    "nodes": [
      {
        "id": "n1",
        "typeId": "device",
        "label": "cam-lobby",
        "x": 80,
        "y": 80,
        "properties": { "device_type": "camera", "data_rate": 90 }
      },
      {
        "id": "n2",
        "typeId": "device",
        "label": "cam-parking",
        "x": 80,
        "y": 220,
        "properties": { "device_type": "camera", "data_rate": 60 }
      },
      {
        "id": "n3",
        "typeId": "edge",
        "label": "edge-01",
        "x": 360,
        "y": 150,
        "properties": { "cpu_cores": 8, "ram_gb": 32 }
      },
      {
        "id": "n4",
        "typeId": "cloud",
        "label": "cloud-analytics",
        "x": 640,
        "y": 150,
        "properties": { "latency_ms": 30 }
      }
    ],
    "edges": [
      { "id": "e1", "from": "n1", "fromSocket": "data", "to": "n3", "toSocket": "data" },
      { "id": "e2", "from": "n2", "fromSocket": "data", "to": "n3", "toSocket": "data" },
      { "id": "e3", "from": "n3", "fromSocket": "data", "to": "n4", "toSocket": "data" }
    ]
  }
}
```

### Example 3 — Complex Topology (7 nodes)

Three heterogeneous devices → two edge processors → one cloud. The edge processors handle different subsets.

```json
{
  "format": "edgemania",
  "version": 1,
  "name": "multi-site-pipeline",
  "savedAt": "2026-08-18T12:00:00Z",
  "graph": {
    "nodes": [
      {
        "id": "n1",
        "typeId": "device",
        "label": "cam-front",
        "x": 80,
        "y": 40,
        "properties": { "device_type": "camera", "data_rate": 95 }
      },
      {
        "id": "n2",
        "typeId": "device",
        "label": "gateway-main",
        "x": 80,
        "y": 170,
        "properties": { "device_type": "gateway", "data_rate": 70 }
      },
      {
        "id": "n3",
        "typeId": "device",
        "label": "thermostat-hvac",
        "x": 80,
        "y": 300,
        "properties": { "device_type": "thermostat", "data_rate": 20 }
      },
      {
        "id": "n4",
        "typeId": "edge",
        "label": "edge-video",
        "x": 360,
        "y": 60,
        "properties": { "cpu_cores": 16, "ram_gb": 64 }
      },
      {
        "id": "n5",
        "typeId": "edge",
        "label": "edge-iot",
        "x": 360,
        "y": 240,
        "properties": { "cpu_cores": 4, "ram_gb": 16 }
      },
      {
        "id": "n6",
        "typeId": "cloud",
        "label": "cloud-central",
        "x": 640,
        "y": 150,
        "properties": { "latency_ms": 50 }
      }
    ],
    "edges": [
      { "id": "e1", "from": "n1", "fromSocket": "data", "to": "n4", "toSocket": "data" },
      { "id": "e2", "from": "n2", "fromSocket": "data", "to": "n4", "toSocket": "data" },
      { "id": "e3", "from": "n2", "fromSocket": "data", "to": "n5", "toSocket": "data" },
      { "id": "e4", "from": "n3", "fromSocket": "data", "to": "n5", "toSocket": "data" },
      { "id": "e5", "from": "n4", "fromSocket": "data", "to": "n6", "toSocket": "data" },
      { "id": "e6", "from": "n5", "fromSocket": "data", "to": "n6", "toSocket": "data" }
    ]
  }
}
```

---

## AI Generation Prompt Template

Copy the text below and paste it into any LLM. Replace `[DESCRIBE YOUR PIPELINE]` with your desired topology.

```
You are an EdgeMania pipeline builder. Generate a valid .em JSON file
based on the user's pipeline description.

RULES:
- The output must be a single JSON object (no markdown fences, no commentary).
- Envelope: {"format":"edgemania","version":1,"name":"...","savedAt":"<ISO-8601 UTC>","graph":{"nodes":[...],"edges":[...]}}
- Node IDs: use "n1", "n2", "n3", ... (unique, sequential).
- Edge IDs: use "e1", "e2", "e3", ... (unique, sequential).
- Node types available:
  - "device": output socket "data", no input sockets.
    Properties: device_type (select: "camera"|"gateway"|"thermostat", default "camera"),
                data_rate (number 1-100, step 1, default 50).
  - "edge": input socket "data", output socket "data".
    Properties: cpu_cores (number 1-64, step 1, default 4),
                ram_gb (number 1-256, step 1, default 16).
  - "cloud": input socket "data", no output sockets.
    Properties: latency_ms (number 1-500, step 1, default 45).
- All edges connect "data" output → "data" input.
- No self-loops (from != to).
- Left-to-right layout: devices x=80, edges x=360, clouds x=640.
- Vertical spacing: 100-140px between nodes in the same column.
- Name: pipeline name using only [A-Za-z0-9 _-], max 64 chars.

USER REQUEST:
[DESCRIBE YOUR PIPELINE]
```

---

## Quick Reference

| typeId | Label | Inputs | Outputs | Properties (defaults) |
|---|---|---|---|---|
| `device` | Device | — | `data` | `device_type: "camera"`, `data_rate: 50` |
| `edge` | Edge | `data` | `data` | `cpu_cores: 4`, `ram_gb: 16` |
| `cloud` | Cloud | `data` | — | `latency_ms: 45` |

**Socket rule:** `fromSocket` must be in the source type's outputs. `toSocket` must be in the target type's inputs. Currently all sockets are named `"data"`.

**Layout convention:**

| Column | x | y spacing | Notes |
|---|---|---|---|
| Devices | 80 | 100–140px apart | Leftmost; source nodes |
| Edges | 360 | 100–140px apart | Middle; processing nodes |
| Clouds | 640 | 100–140px apart | Rightmost; output nodes |
