/* EdgeMania.io — graph.js  Canvas interactions, state, toolbar, drawer. */
const graph = (() => {
    /* ── State ────────────────────────────────────────────── */
    let canvasNodes = [];   // {id, typeId, label, x, y, properties, el}
    let canvasEdges = [];   // {id, from, fromSocket, to, toSocket, el}
    let selection = null;   // node id
    let lastRun = null;     // simulation result

    /* ── Init ─────────────────────────────────────────────── */
    async function init() {
        const paletteList = document.getElementById('palette-list');
        await nodes.loadPalette(paletteList);
        bindPalette(paletteList);
        bindCanvas();
        bindToolbar();
        bindKeyboard();
    }

    /* ── Palette drag → add node ──────────────────────────── */
    function bindPalette(paletteList) {
        let dragging = null; // {typeId, ghost}

        paletteList.addEventListener('mousedown', (e) => {
            const item = e.target.closest('.palette__item');
            if (!item) return;
            e.preventDefault();

            const typeId = item.dataset.typeId;
            const type = nodes.getNodeTypes().find(t => t.id === typeId);
            if (!type) return;

            const ghost = document.createElement('div');
            ghost.className = 'node node--ghost';
            ghost.innerHTML = `<div class="node__header" style="background:var(--${type.color})">
                <span class="node__label">${type.label}</span></div>`;
            ghost.style.position = 'fixed';
            ghost.style.pointerEvents = 'none';
            ghost.style.zIndex = '1000';
            ghost.style.opacity = '0.8';
            document.body.appendChild(ghost);

            dragging = { typeId, ghost, type };
            moveGhost(e);
        });

        document.addEventListener('mousemove', (e) => {
            if (dragging) moveGhost(e);
        });

        document.addEventListener('mouseup', (e) => {
            if (!dragging) return;
            dragging.ghost.remove();

            const canvas = document.getElementById('canvas');
            const rect = canvas.getBoundingClientRect();
            if (e.clientX >= rect.left && e.clientX <= rect.right &&
                e.clientY >= rect.top && e.clientY <= rect.bottom) {
                const x = snap(e.clientX - rect.left + canvas.scrollLeft - 40);
                const y = snap(e.clientY - rect.top + canvas.scrollTop - 20);
                addNode(dragging.typeId, x, y);
            }
            dragging = null;
        });

        function moveGhost(e) {
            dragging.ghost.style.left = (e.clientX - 40) + 'px';
            dragging.ghost.style.top = (e.clientY - 20) + 'px';
        }
    }

    /* ── Add / remove nodes ───────────────────────────────── */
    async function addNode(typeId, x, y, props) {
        const body = { typeId, x, y, properties: props || {} };
        const created = await api.post('/api/nodes', body);
        const type = nodes.getNodeTypes().find(t => t.id === typeId);
        created.label = created.label || typeId;
        const el = nodes.renderNode(created, document.getElementById('canvas-nodes'));
        canvasNodes.push({ ...created, el });
        selectNode(created.id);
        return created;
    }

    function removeNode(id) {
        api.del('/api/nodes/' + id).catch(() => {});
        nodes.removeNodeDOM(id, document.getElementById('canvas-nodes'));
        canvasEdges = canvasEdges.filter(e => {
            if (e.from === id || e.to === id) {
                if (e.el) e.el.remove();
                return false;
            }
            return true;
        });
        canvasNodes = canvasNodes.filter(n => n.id !== id);
        if (selection === id) { selection = null; clearDrawer(); }
    }

    /* ── Canvas interactions ──────────────────────────────── */
    function bindCanvas() {
        const canvasEl = document.getElementById('canvas');
        const nodesEl = document.getElementById('canvas-nodes');
        const edgesEl = document.getElementById('canvas-edges');

        let dragState = null;   // {nodeId, startX, startY, origX, origY}
        let connectState = null; // {fromId, fromSocket, tempLine}

        // Click on canvas background → deselect
        canvasEl.addEventListener('mousedown', (e) => {
            if (e.target === canvasEl || e.target === nodesEl || e.target === edgesEl) {
                selectNode(null);
            }
        });

        // Node interactions: mousedown on node → start drag; mousedown on socket → start connect
        nodesEl.addEventListener('mousedown', (e) => {
            const socket = e.target.closest('.socket');
            const nodeEl = e.target.closest('.node');
            if (!nodeEl) return;

            if (socket) {
                e.stopPropagation();
                const direction = socket.dataset.direction;
                const socketName = socket.dataset.name;
                const nodeId = nodeEl.dataset.id;

                if (direction === 'output') {
                    // Start connecting from output
                    const line = createTempLine();
                    edgesEl.appendChild(line);
                    connectState = { fromId: nodeId, fromSocket: socketName, tempLine: line };
                    const pos = getSocketPagePos(socket);
                    connectState.startX = pos.x;
                    connectState.startY = pos.y;
                }
                return;
            }

            // Click on node body → select + start drag
            e.stopPropagation();
            selectNode(nodeEl.dataset.id);

            const node = canvasNodes.find(n => n.id === nodeEl.dataset.id);
            if (!node) return;
            dragState = {
                nodeId: node.id,
                startX: e.clientX,
                startY: e.clientY,
                origX: node.x,
                origY: node.y,
            };
        });

        // Mouse move: drag node or draw temp connection line
        document.addEventListener('mousemove', (e) => {
            if (dragState) {
                const dx = e.clientX - dragState.startX;
                const dy = e.clientY - dragState.startY;
                const newX = snap(dragState.origX + dx);
                const newY = snap(dragState.origY + dy);
                const node = canvasNodes.find(n => n.id === dragState.nodeId);
                if (node) {
                    node.x = newX;
                    node.y = newY;
                    nodes.updateNodePosition(node.id, newX, newY);
                    updateEdgesForNode(node.id);
                }
            }
            if (connectState) {
                const canvasRect = canvasEl.getBoundingClientRect();
                const mx = e.clientX - canvasRect.left + canvasEl.scrollLeft;
                const my = e.clientY - canvasRect.top + canvasEl.scrollTop;
                setLineEnd(connectState.tempLine, connectState.startX, connectState.startY, mx, my);
            }
        });

        // Mouse up: finish drag (sync to backend) or finish connect
        document.addEventListener('mouseup', (e) => {
            if (dragState) {
                const node = canvasNodes.find(n => n.id === dragState.nodeId);
                if (node) {
                    api.put('/api/nodes/' + node.id, { properties: node.properties }).catch(() => {});
                }
                dragState = null;
            }
            if (connectState) {
                connectState.tempLine.remove();
                const target = document.elementFromPoint(e.clientX, e.clientY);
                const targetSocket = target && target.closest('.socket');
                const targetNode = target && target.closest('.node');

                if (targetSocket && targetNode &&
                    targetSocket.dataset.direction === 'input' &&
                    targetNode.dataset.id !== connectState.fromId) {
                    addEdge(connectState.fromId, connectState.fromSocket,
                            targetNode.dataset.id, targetSocket.dataset.name);
                }
                connectState = null;
            }
        });
    }

    /* ── Edges ────────────────────────────────────────────── */
    function addEdge(fromId, fromSocket, toId, toSocket) {
        // Prevent duplicate
        const exists = canvasEdges.some(e =>
            e.from === fromId && e.fromSocket === fromSocket &&
            e.to === toId && e.toSocket === toSocket);
        if (exists) return;

        const ns = 'http://www.w3.org/2000/svg';
        const path = document.createElementNS(ns, 'path');
        path.setAttribute('stroke', 'var(--ink-black)');
        path.setAttribute('stroke-width', '2.5');
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke-linecap', 'round');
        document.getElementById('canvas-edges').appendChild(path);

        const edge = {
            id: 'e-' + Math.random().toString(36).slice(2, 8),
            from: fromId, fromSocket, to: toId, toSocket, el: path,
        };
        canvasEdges.push(edge);
        drawEdge(edge);
    }

    function drawEdge(edge) {
        const from = nodes.getNodeSocketCenter(edge.from, 'output', edge.fromSocket);
        const to = nodes.getNodeSocketCenter(edge.to, 'input', edge.toSocket);
        if (!from || !to) return;
        edge.el.setAttribute('d', wavyPath(from.x, from.y, to.x, to.y));
    }

    function updateEdgesForNode(nodeId) {
        canvasEdges.forEach(e => {
            if (e.from === nodeId || e.to === nodeId) drawEdge(e);
        });
    }

    function wavyPath(x1, y1, x2, y2) {
        const dx = x2 - x1;
        const dy = y2 - y1;
        const dist = Math.sqrt(dx * dx + dy * dy);
        const jitter = Math.min(dist * 0.15, 12);
        const j1 = (Math.random() - 0.5) * jitter;
        const j2 = (Math.random() - 0.5) * jitter;
        const j3 = (Math.random() - 0.5) * jitter;
        const j4 = (Math.random() - 0.5) * jitter;
        const cx1 = x1 + dx * 0.33 + j1;
        const cy1 = y1 + dy * 0.33 + j2;
        const cx2 = x1 + dx * 0.66 + j3;
        const cy2 = y1 + dy * 0.66 + j4;
        return `M${x1},${y1} C${cx1},${cy1} ${cx2},${cy2} ${x2},${y2}`;
    }

    function createTempLine() {
        const ns = 'http://www.w3.org/2000/svg';
        const line = document.createElementNS(ns, 'line');
        line.setAttribute('stroke', 'var(--primary-container)');
        line.setAttribute('stroke-width', '2');
        line.setAttribute('stroke-dasharray', '6 4');
        line.setAttribute('pointer-events', 'none');
        return line;
    }

    function setLineEnd(line, x1, y1, x2, y2) {
        line.setAttribute('x1', x1);
        line.setAttribute('y1', y1);
        line.setAttribute('x2', x2);
        line.setAttribute('y2', y2);
    }

    function getSocketPagePos(socketEl) {
        const rect = socketEl.getBoundingClientRect();
        return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
    }

    /* ── Selection ────────────────────────────────────────── */
    function selectNode(id) {
        selection = id;
        nodes.highlightNode(id, true);
        if (id) {
            const node = canvasNodes.find(n => n.id === id);
            if (node) populateDrawer(node);
        } else {
            clearDrawer();
        }
    }

    /* ── Keyboard ─────────────────────────────────────────── */
    function bindKeyboard() {
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Delete' || e.key === 'Backspace') {
                if (document.activeElement && document.activeElement.tagName === 'INPUT') return;
                if (selection) removeNode(selection);
            }
        });
    }

    /* ── Toolbar ──────────────────────────────────────────── */
    function bindToolbar() {
        document.getElementById('btn-new').addEventListener('click', clearCanvas);
        document.getElementById('btn-sample').addEventListener('click', loadSample);
        document.getElementById('btn-run').addEventListener('click', runSimulation);
    }

    function clearCanvas() {
        canvasNodes.forEach(n => {
            nodes.removeNodeDOM(n.id, document.getElementById('canvas-nodes'));
        });
        canvasEdges.forEach(e => { if (e.el) e.el.remove(); });
        canvasNodes = [];
        canvasEdges = [];
        selection = null;
        clearDrawer();
        nodes.clearNodeValues();
        setToolbarStatus('');
    }

    async function loadSample() {
        clearCanvas();
        const instances = await api.post('/api/nodes/sample');
        const idMap = {};

        instances.forEach(inst => {
            const type = nodes.getNodeTypes().find(t => t.id === inst.typeId);
            inst.label = inst.label || inst.typeId;
            const el = nodes.renderNode(inst, document.getElementById('canvas-nodes'));
            canvasNodes.push({ ...inst, el });
            // Map original sample index to new id
            const idx = instances.indexOf(inst);
            idMap[idx] = inst.id;
        });

        // Create predefined edges using mapped ids
        // Sample graph: device→edge, edge→cloud
        const edgeDefs = [
            [0, 'data', 1, 'data'],
            [1, 'data', 2, 'data'],
        ];
        edgeDefs.forEach(([fromIdx, fromSock, toIdx, toSock]) => {
            if (idMap[fromIdx] && idMap[toIdx]) {
                addEdge(idMap[fromIdx], fromSock, idMap[toIdx], toSock);
            }
        });

        setToolbarStatus('Sample loaded');
    }

    async function runSimulation() {
        if (canvasNodes.length === 0) {
            setToolbarStatus('Nothing to run — add nodes first');
            return;
        }

        setToolbarStatus('Running\u2026');
        document.getElementById('btn-run').disabled = true;

        try {
            const graphDto = {
                nodes: canvasNodes.map(n => ({
                    id: n.id,
                    typeId: n.typeId,
                    label: n.label,
                    x: n.x,
                    y: n.y,
                    properties: n.properties || {},
                })),
                edges: canvasEdges.map(e => ({
                    id: e.id,
                    from: e.from,
                    fromSocket: e.fromSocket,
                    to: e.to,
                    toSocket: e.toSocket,
                })),
            };

            const result = await api.post('/api/simulations/run', {
                graph: graphDto,
                ticks: 100,
                tickMs: 100,
            });

            lastRun = result;

            // Show values on nodes
            nodes.clearNodeValues();
            result.nodeOutputs.forEach(out => {
                nodes.showNodeValue(out.nodeId, out.lastValue, out.status);
            });

            // Update drawer if a node is selected
            if (selection) {
                const out = result.nodeOutputs.find(o => o.nodeId === selection);
                if (out) updateDrawerStatus(out);
            }

            setToolbarStatus(`Completed \u00b7 ${result.ticks} ticks`);
        } catch (err) {
            setToolbarStatus('Error: ' + err.message);
        } finally {
            document.getElementById('btn-run').disabled = false;
        }
    }

    function setToolbarStatus(msg) {
        document.getElementById('toolbar-status').textContent = msg;
    }

    /* ── Property Drawer ──────────────────────────────────── */
    function populateDrawer(node) {
        const body = document.getElementById('drawer-body');
        const type = nodes.getNodeTypes().find(t => t.id === node.typeId);
        if (!type) return;

        body.innerHTML = '';

        // Header
        const colorVar = `var(--${type.color})`;
        const header = document.createElement('div');
        header.className = 'drawer__header';
        header.innerHTML = `
            <span class="drawer__type-dot" style="background:${colorVar}"></span>
            <span class="drawer__node-label">${node.label}</span>
            <span class="drawer__type-id">${type.label}</span>`;
        body.appendChild(header);

        // Fields
        type.properties.forEach(ps => {
            const field = document.createElement('div');
            field.className = 'field';

            const lbl = document.createElement('label');
            lbl.className = 'field__label';
            lbl.textContent = ps.label;
            lbl.setAttribute('for', `prop-${ps.key}`);
            field.appendChild(lbl);

            const currentVal = node.properties[ps.key] ?? ps.defaultVal;

            if (ps.type === 'number') {
                const input = document.createElement('input');
                input.className = 'input input--numeric';
                input.id = `prop-${ps.key}`;
                input.type = 'text';
                input.readOnly = true; // we handle mouse drag
                input.dataset.key = ps.key;
                input.dataset.min = ps.min;
                input.dataset.max = ps.max;
                input.dataset.step = ps.step;
                input.value = Number(currentVal).toFixed(1);
                updateFillPercent(input, currentVal, ps.min, ps.max);
                bindDragNumeric(input, node);
                field.appendChild(input);
            } else if (ps.type === 'select') {
                const select = document.createElement('select');
                select.className = 'input';
                select.id = `prop-${ps.key}`;
                select.dataset.key = ps.key;
                (ps.options || []).forEach(opt => {
                    const o = document.createElement('option');
                    o.value = opt;
                    o.textContent = opt;
                    if (opt === currentVal) o.selected = true;
                    select.appendChild(o);
                });
                field.appendChild(select);
            } else if (ps.type === 'boolean') {
                const toggle = document.createElement('div');
                toggle.className = 'toggle';
                toggle.id = `prop-${ps.key}`;
                toggle.dataset.key = ps.key;
                toggle.setAttribute('role', 'switch');
                toggle.setAttribute('aria-checked', currentVal ? 'true' : 'false');
                toggle.tabIndex = 0;
                toggle.addEventListener('click', () => {
                    const checked = toggle.getAttribute('aria-checked') === 'true';
                    toggle.setAttribute('aria-checked', (!checked).toString());
                });
                toggle.addEventListener('keydown', (e) => {
                    if (e.key === ' ' || e.key === 'Enter') {
                        e.preventDefault();
                        toggle.click();
                    }
                });
                field.appendChild(toggle);
            }

            body.appendChild(field);
        });

        // Save button
        const saveBtn = document.createElement('button');
        saveBtn.className = 'btn btn--primary btn--toolbar';
        saveBtn.textContent = 'Save';
        saveBtn.addEventListener('click', () => saveDrawer(node));
        body.appendChild(saveBtn);

        // Status footer
        const statusEl = document.createElement('div');
        statusEl.className = 'drawer__status';
        statusEl.id = 'drawer-status';
        if (lastRun) {
            const out = lastRun.nodeOutputs.find(o => o.nodeId === node.id);
            if (out) {
                statusEl.textContent = `Last value: ${out.lastValue.toFixed(2)}`;
            }
        }
        body.appendChild(statusEl);
    }

    function saveDrawer(node) {
        const body = document.getElementById('drawer-body');
        const fields = body.querySelectorAll('[data-key]');
        const props = {};
        fields.forEach(f => {
            const key = f.dataset.key;
            if (f.classList.contains('toggle')) {
                props[key] = f.getAttribute('aria-checked') === 'true';
            } else if (f.tagName === 'SELECT') {
                props[key] = f.value;
            } else if (f.classList.contains('input--numeric')) {
                props[key] = parseFloat(f.value);
            } else {
                props[key] = f.value;
            }
        });
        node.properties = props;
        api.put('/api/nodes/' + node.id, { properties: props }).catch(() => {});
        setToolbarStatus('Saved');
    }

    function updateDrawerStatus(out) {
        const el = document.getElementById('drawer-status');
        if (el) el.textContent = `Last value: ${out.lastValue.toFixed(2)}`;
    }

    function clearDrawer() {
        const body = document.getElementById('drawer-body');
        body.innerHTML = '<p class="drawer__empty">Select a node to edit its properties.</p>';
    }

    /* ── Custom drag-to-adjust numerics ───────────────────── */
    function bindDragNumeric(input, node) {
        let dragging = null;

        input.addEventListener('mousedown', (e) => {
            e.preventDefault();
            const min = parseFloat(input.dataset.min);
            const max = parseFloat(input.dataset.max);
            const step = parseFloat(input.dataset.step);
            const range = max - min;
            const startVal = parseFloat(input.value);
            dragging = { startX: e.clientX, startVal, min, max, step, range, input };
        });

        document.addEventListener('mousemove', (e) => {
            if (!dragging || dragging.input !== input) return;
            const dx = e.clientX - dragging.startX;
            const pixelsPerStep = 4;
            const stepsChanged = Math.round(dx / pixelsPerStep);
            let newVal = dragging.startVal + stepsChanged * dragging.step;
            newVal = Math.max(dragging.min, Math.min(dragging.max, newVal));
            newVal = Math.round(newVal / dragging.step) * dragging.step;
            dragging.input.value = newVal.toFixed(1);
            updateFillPercent(dragging.input, newVal, dragging.min, dragging.max);
        });

        document.addEventListener('mouseup', () => {
            if (dragging && dragging.input === input) dragging = null;
        });
    }

    function updateFillPercent(input, value, min, max) {
        const pct = ((value - min) / (max - min)) * 100;
        input.style.setProperty('--fill', pct + '%');
    }

    /* ── Grid snap ────────────────────────────────────────── */
    function snap(v) {
        return Math.round(v / 4) * 4;
    }

    /* ── Public ───────────────────────────────────────────── */
    return { init };
})();

/* Boot */
document.addEventListener('DOMContentLoaded', () => graph.init());
