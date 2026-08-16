/* EdgeMania.io — dashboard.js  Security Dashboard: tiles, sparklines, event feed, polling. */
const dashboard = (() => {
    const POLL_MS = 5000;
    const SVG_NS = 'http://www.w3.org/2000/svg';
    const CHART = { w: 600, h: 200, pad: { top: 10, right: 10, bottom: 20, left: 40 } };
    const SERIES = [
        { key: 'cpu',    cls: 'cpu',    label: 'CPU %' },
        { key: 'memory', cls: 'memory', label: 'Memory %' },
        { key: 'latency', cls: 'latency', label: 'Latency ms' },
    ];

    let pollTimer = null;
    let running = false;

    /* ── Init ─────────────────────────────────────────────── */
    async function init() {
        await refresh();
        startPoll();
        document.addEventListener('visibilitychange', onVisibility);
    }

    /* ── Polling ──────────────────────────────────────────── */
    function startPoll() {
        stopPoll();
        pollTimer = setInterval(refresh, POLL_MS);
        running = true;
    }

    function stopPoll() {
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        running = false;
    }

    function onVisibility() {
        if (document.hidden) {
            stopPoll();
        } else {
            refresh();
            startPoll();
        }
    }

    /* ── Refresh cycle ────────────────────────────────────── */
    async function refresh() {
        try {
            const [snapshot, history] = await Promise.all([
                api.get('/api/dashboard/metrics'),
                api.get('/api/dashboard/metrics/history?window=5m'),
            ]);
            renderTiles(snapshot);
            renderChart(history.points);
            renderFeed(snapshot.events);
            renderFooter();
        } catch (e) {
            console.error('Dashboard refresh failed:', e);
        }
    }

    /* ── KPI Tiles ────────────────────────────────────────── */
    function renderTiles(s) {
        setText('kpi-cpu', s.cpu.value);
        setText('kpi-memory', s.memory.value);
        setText('kpi-latency', s.latencyP95.value);
        setText('kpi-nodes', s.nodes);
        setText('kpi-simulations', s.simulations);
        setText('kpi-events', s.events24h);
    }

    function setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    }

    /* ── SVG Sparkline Charts ─────────────────────────────── */
    function renderChart(points) {
        if (!points || points.length < 2) return;
        const svg = document.getElementById('chart-svg');
        if (!svg) return;
        svg.innerHTML = '';

        /* defs: hatch pattern */
        const defs = createEl('defs');
        const pattern = createEl('pattern', {
            id: 'hatch-fill', width: 6, height: 6,
            patternUnits: 'userSpaceOnUse',
            patternTransform: 'rotate(45)',
        });
        pattern.appendChild(createEl('line', {
            x1: 0, y1: 0, x2: 0, y2: 6,
            stroke: 'var(--ink-black)', 'stroke-width': 1, opacity: 0.15,
        }));
        defs.appendChild(pattern);
        svg.appendChild(defs);

        const plotW = CHART.w - CHART.pad.left - CHART.pad.right;
        const plotH = CHART.h - CHART.pad.top - CHART.pad.bottom;

        /* Grid lines */
        const gridG = createEl('g', { class: 'chart-grid' });
        for (let i = 0; i <= 4; i++) {
            const y = CHART.pad.top + (plotH / 4) * i;
            gridG.appendChild(createEl('line', {
                x1: CHART.pad.left, y1: y, x2: CHART.w - CHART.pad.right, y2: y,
                class: 'grid-line',
            }));
        }
        svg.appendChild(gridG);

        /* Y-axis labels (0-100 for CPU/Memory) */
        const labelsG = createEl('g');
        for (let i = 0; i <= 4; i++) {
            const y = CHART.pad.top + (plotH / 4) * i;
            const val = 100 - (100 / 4) * i;
            const t = createEl('text', {
                x: CHART.pad.left - 6, y: y + 3,
                'text-anchor': 'end', class: 'chart-label',
            });
            t.textContent = Math.round(val);
            labelsG.appendChild(t);
        }
        svg.appendChild(labelsG);

        /* Draw each series */
        const seriesData = {
            cpu: points.map(p => p.cpu),
            memory: points.map(p => p.memory),
            latency: points.map(p => p.latency),
        };

        const yMax = { cpu: 100, memory: 100, latency: 60 };

        for (const s of SERIES) {
            const vals = seriesData[s.key];
            const max = yMax[s.key];
            const coords = vals.map((v, i) => {
                const x = CHART.pad.left + (i / (vals.length - 1)) * plotW;
                const y = CHART.pad.top + plotH - (v / max) * plotH;
                return { x, y };
            });

            /* Area fill */
            const areaPath = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x},${c.y}`).join(' ');
            const areaD = areaPath + ` L${coords[coords.length - 1].x},${CHART.pad.top + plotH} L${coords[0].x},${CHART.pad.top + plotH} Z`;
            svg.appendChild(createEl('path', {
                d: areaD, class: `sparkline-area sparkline-area--${s.cls}`,
                fill: 'url(#hatch-fill)',
            }));

            /* Line */
            const linePath = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x},${c.y}`).join(' ');
            const line = createEl('path', {
                d: linePath, class: `sparkline-line sparkline-line--${s.cls}`,
            });
            if (!theme.prefersReducedMotion()) {
                const len = line.getTotalLength ? 1500 : 0;
                if (len) {
                    line.style.strokeDasharray = len;
                    line.style.strokeDashoffset = len;
                    line.style.transition = 'stroke-dashoffset 400ms ease-out';
                    requestAnimationFrame(() => { line.style.strokeDashoffset = '0'; });
                }
            }
            svg.appendChild(line);
        }

        /* X-axis time labels */
        const xLabels = createEl('g');
        const step = Math.max(1, Math.floor(points.length / 5));
        for (let i = 0; i < points.length; i += step) {
            const x = CHART.pad.left + (i / (points.length - 1)) * plotW;
            const d = new Date(points[i].t);
            const label = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
            const t = createEl('text', {
                x, y: CHART.h - 4, 'text-anchor': 'middle', class: 'chart-label',
            });
            t.textContent = label;
            xLabels.appendChild(t);
        }
        svg.appendChild(xLabels);
    }

    /* ── Event Feed ───────────────────────────────────────── */
    function renderFeed(events) {
        const list = document.getElementById('event-list');
        if (!list) return;
        list.innerHTML = '';
        if (!events || events.length === 0) {
            list.innerHTML = '<div class="event-row"><span class="event-row__msg" style="color:var(--on-surface-variant)">No events yet</span></div>';
            return;
        }
        for (const ev of events) {
            const row = document.createElement('div');
            row.className = `event-row event-row--${ev.severity}`;
            row.innerHTML = `
                <span class="event-row__dot"></span>
                <span class="event-row__time">${ev.time}</span>
                <span class="event-row__msg">${ev.message}</span>
            `;
            list.appendChild(row);
        }
    }

    /* ── Footer ───────────────────────────────────────────── */
    function renderFooter() {
        const el = document.getElementById('last-refresh');
        if (!el) return;
        const now = new Date();
        const t = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
        el.textContent = `Last updated: ${t}`;
    }

    /* ── SVG helpers ──────────────────────────────────────── */
    function createEl(tag, attrs) {
        const el = document.createElementNS(SVG_NS, tag);
        if (attrs) {
            for (const [k, v] of Object.entries(attrs)) {
                el.setAttribute(k, v);
            }
        }
        return el;
    }

    return { init };
})();

document.addEventListener('DOMContentLoaded', () => dashboard.init());
