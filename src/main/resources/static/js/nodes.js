/* EdgeMania.io — nodes.js  Palette + canvas node rendering. */
const nodes = (() => {
    let nodeTypes = [];

    /* ── Palette ──────────────────────────────────────────── */

    async function loadPalette(listEl) {
        const data = await api.get('/api/nodes');
        nodeTypes = data.types;
        listEl.innerHTML = '';
        if (!nodeTypes || nodeTypes.length === 0) {
            const empty = document.createElement('p');
            empty.className = 'palette__empty';
            empty.textContent = 'No node types available';
            listEl.appendChild(empty);
            return nodeTypes;
        }
        nodeTypes.forEach(type => {
            const item = document.createElement('div');
            item.className = 'palette__item';
            item.dataset.typeId = type.id;
            item.setAttribute('draggable', 'false'); // we handle mousedown ourselves
            item.setAttribute('role', 'button');
            item.setAttribute('aria-label', `Add ${type.label} node`);
            item.tabIndex = 0;

            const swatch = document.createElement('span');
            swatch.className = 'palette__swatch';
            const colorVar = `var(--${type.color})`;
            swatch.style.background = colorVar;

            const label = document.createElement('span');
            label.className = 'palette__label';
            label.textContent = type.label;

            item.appendChild(swatch);
            item.appendChild(label);
            listEl.appendChild(item);
        });
        return nodeTypes;
    }

    function getNodeTypes() { return nodeTypes; }

    /* ── Canvas node rendering ────────────────────────────── */

    function renderNode(nodeData, canvasNodesEl) {
        const el = document.createElement('div');
        el.className = 'node';
        el.dataset.id = nodeData.id;
        el.style.left = nodeData.x + 'px';
        el.style.top = nodeData.y + 'px';

        const type = nodeTypes.find(t => t.id === nodeData.typeId) || {};
        const colorVar = `var(--${type.color || 'outline'})`;

        // Header
        const header = document.createElement('div');
        header.className = 'node__header';
        header.style.background = colorVar;
        header.setAttribute('aria-hidden', 'true');

        const label = document.createElement('span');
        label.className = 'node__label';
        label.textContent = nodeData.label || nodeData.typeId;
        header.appendChild(label);
        el.appendChild(header);

        // Sockets container
        const socketsWrap = document.createElement('div');
        socketsWrap.className = 'node__sockets';

        // Input sockets (left side)
        const inputSockets = (type.sockets && type.sockets.inputs) || [];
        inputSockets.forEach((socketName, i) => {
            const socket = createSocketDOM('input', socketName, 'data', i, inputSockets.length);
            socketsWrap.appendChild(socket);
        });

        // Output sockets (right side)
        const outputSockets = (type.sockets && type.sockets.outputs) || [];
        outputSockets.forEach((socketName, i) => {
            const socket = createSocketDOM('output', socketName, 'data', i, outputSockets.length);
            socketsWrap.appendChild(socket);
        });

        el.appendChild(socketsWrap);

        // Value badge (shown after simulation)
        const badge = document.createElement('div');
        badge.className = 'node__badge';
        badge.style.display = 'none';
        el.appendChild(badge);

        canvasNodesEl.appendChild(el);
        return el;
    }

    function createSocketDOM(direction, name, type, index, total) {
        const socket = document.createElement('div');
        socket.className = `socket socket--${direction}`;
        socket.dataset.name = name;
        socket.dataset.type = type;
        socket.dataset.direction = direction;

        // Position sockets vertically along the node edges
        const spacing = 28;
        const startY = 40; // below header
        if (direction === 'input') {
            socket.style.left = '-6px';
            socket.style.top = (startY + index * spacing) + 'px';
        } else {
            socket.style.right = '-6px';
            socket.style.top = (startY + index * spacing) + 'px';
        }

        // Hex clip shape via clip-path
        const pts = [];
        for (let i = 0; i < 6; i++) {
            const angle = (Math.PI / 3) * i - Math.PI / 6;
            pts.push(`${50 + 50 * Math.cos(angle)}% ${50 + 50 * Math.sin(angle)}%`);
        }
        socket.style.clipPath = `polygon(${pts.join(', ')})`;

        // Socket color by type
        const colors = {
            control: 'var(--primary-container)',
            data:    'var(--tertiary)',
            output:  'var(--secondary)',
            boolean: 'var(--secondary-container)',
        };
        socket.style.background = colors[type] || 'var(--outline)';

        return socket;
    }

    /* ── Helpers ──────────────────────────────────────────── */

    function removeNodeDOM(id, canvasNodesEl) {
        const el = canvasNodesEl.querySelector(`.node[data-id="${id}"]`);
        if (el) el.remove();
    }

    function updateNodePosition(id, x, y) {
        const el = document.querySelector(`.node[data-id="${id}"]`);
        if (el) {
            el.style.left = x + 'px';
            el.style.top = y + 'px';
        }
    }

    function getNodeSocketCenter(nodeId, direction, socketName) {
        const nodeEl = document.querySelector(`.node[data-id="${nodeId}"]`);
        if (!nodeEl) return null;
        const socket = nodeEl.querySelector(
            `.socket[data-direction="${direction}"][data-name="${socketName}"]`);
        if (!socket) return null;
        const nodeRect = nodeEl.getBoundingClientRect();
        const socketRect = socket.getBoundingClientRect();
        const canvasEl = document.getElementById('canvas');
        const canvasRect = canvasEl.getBoundingClientRect();
        return {
            x: socketRect.left + socketRect.width / 2 - canvasRect.left + canvasEl.scrollLeft,
            y: socketRect.top + socketRect.height / 2 - canvasRect.top + canvasEl.scrollTop,
        };
    }

    function highlightNode(id, selected) {
        document.querySelectorAll('.node').forEach(el => {
            el.classList.toggle('node--selected', el.dataset.id === id && selected);
        });
    }

    function showNodeValue(nodeId, value, status) {
        const el = document.querySelector(`.node[data-id="${nodeId}"]`);
        if (!el) return;
        const badge = el.querySelector('.node__badge');
        if (badge) {
            badge.textContent = typeof value === 'number' ? value.toFixed(1) : value;
            badge.style.display = '';
        }
        // Overload warning
        const header = el.querySelector('.node__header');
        if (header) {
            const existing = header.querySelector('.node__overload');
            if (status === 'overload') {
                if (!existing) {
                    const warn = document.createElement('span');
                    warn.className = 'node__overload';
                    warn.textContent = '!';
                    warn.title = 'Node overloaded';
                    header.appendChild(warn);
                }
                el.classList.add('node--overload');
            } else {
                if (existing) existing.remove();
                el.classList.remove('node--overload');
            }
        }
    }

    function clearNodeValues() {
        document.querySelectorAll('.node__badge').forEach(b => b.style.display = 'none');
    }

    return {
        loadPalette,
        getNodeTypes,
        renderNode,
        removeNodeDOM,
        updateNodePosition,
        getNodeSocketCenter,
        highlightNode,
        showNodeValue,
        clearNodeValues,
    };
})();
