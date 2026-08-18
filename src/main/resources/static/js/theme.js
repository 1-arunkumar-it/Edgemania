/* EdgeMania.io — theme.js  Runtime helpers. */
const theme = (() => {
    function prefersReducedMotion() {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    /* Create an SVG hex socket element. */
    function createHexSocket(type) {
        const ns = 'http://www.w3.org/2000/svg';
        const g = document.createElementNS(ns, 'g');
        const poly = document.createElementNS(ns, 'polygon');
        const size = 6;
        const pts = [];
        for (let i = 0; i < 6; i++) {
            const angle = (Math.PI / 3) * i - Math.PI / 6;
            pts.push(`${size * Math.cos(angle)},${size * Math.sin(angle)}`);
        }
        poly.setAttribute('points', pts.join(' '));
        poly.setAttribute('stroke', 'var(--ink-black)');
        poly.setAttribute('stroke-width', '2');
        const colors = {
            control: 'var(--primary-container)',
            data:    'var(--tertiary)',
            output:  'var(--secondary)',
            boolean: 'var(--secondary-container)',
        };
        poly.setAttribute('fill', colors[type] || 'var(--outline)');
        g.appendChild(poly);
        return g;
    }

    /* ── Manga-sketch node icons ──────────────────────────────
       Hand-drawn SVG strokes: irregular weights, slight jitter,
       ink-on-paper feel per DESIGN.md §1 "Inked Authenticity". */

    const _icons = {
        /* Device: Camera — box body + circle lens + viewfinder bump */
        camera: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 8.5h1.5l1.2-2.8h12.6L19.5 8.5H21a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1v-10a1 1 0 0 1 1-1z"/>
            <circle cx="12" cy="14" r="3.8" stroke-width="2.2"/>
            <circle cx="12" cy="14" r="1.2"/>
            <path d="M16.5 9.8v-1.3" stroke-width="1.5"/>
        </svg>`,

        /* Device: Gateway — antenna tower + signal arcs */
        gateway: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 21V10"/>
            <path d="M8 21h8"/>
            <path d="M10 10l2-4 2 4"/>
            <path d="M7.2 7.5a6.5 6.5 0 0 1 9.6 0" stroke-width="1.5"/>
            <path d="M5 5.2a10 10 0 0 1 14 0" stroke-width="1.3"/>
            <circle cx="12" cy="10.5" r="1" fill="var(--ink-black)" stroke="none"/>
        </svg>`,

        /* Device: Thermostat — circle dial + tick marks + wavy line */
        thermostat: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12.5" r="8.5"/>
            <circle cx="12" cy="12.5" r="5.5" stroke-width="1.3"/>
            <path d="M12 4.5v1.5M12 19v1.5M4.5 12H6M18 12h1.5" stroke-width="1.5"/>
            <path d="M9.5 12.5c.8-1.5 1.5-1 2 0s1.2 1.5 2 0 .8-1 1.2-.5" stroke-width="1.5"/>
        </svg>`,

        /* Edge: Chip — square IC package + pin legs */
        chip: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="6" y="6" width="12" height="12" rx="1"/>
            <rect x="8.5" y="8.5" width="7" height="7" rx="0.5" stroke-width="1.3"/>
            <path d="M9.5 6V3.5M14.5 6V3.5M9.5 20.5V18M14.5 20.5V18M6 9.5H3.5M6 14.5H3.5M20.5 9.5H18M20.5 14.5H18" stroke-width="1.5"/>
        </svg>`,

        /* Cloud: PC — monitor + stand + screen glare */
        'cloud-pc': `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2.5" y="4" width="19" height="13" rx="1.5"/>
            <path d="M9 21h6M12 17v4"/>
            <path d="M6 8h2.5" stroke-width="1.5" opacity="0.5"/>
            <path d="M6 10.5h5" stroke-width="1" opacity="0.35"/>
        </svg>`,

        /* Feature: Compose — 3 nodes with wavy edges */
        compose: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="8" width="6" height="5" rx="1"/>
            <rect x="16" y="4" width="6" height="5" rx="1"/>
            <rect x="16" y="15" width="6" height="5" rx="1"/>
            <path d="M8 10.5c2-2 4-1.5 5 0" stroke-width="1.5"/>
            <path d="M8 10.5c2 2.5 4 2 5 0.5" stroke-width="1.5"/>
        </svg>`,

        /* Feature: Tune — sliders with ink marks */
        tune: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 7h16M4 17h16" stroke-width="1.3"/>
            <rect x="7" y="5" width="3" height="4" rx="0.5" fill="var(--ink-black)" fill-opacity="0.15"/>
            <rect x="14" y="15" width="3" height="4" rx="0.5" fill="var(--ink-black)" fill-opacity="0.15"/>
            <circle cx="9" cy="7" r="2.2"/>
            <circle cx="16" cy="17" r="2.2"/>
        </svg>`,

        /* Feature: Simulate — lightning bolt */
        simulate: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M13 2L4.5 13.5h7L9.5 22 19.5 10.5h-7L13 2z" stroke-width="2.2"/>
        </svg>`,

        /* Feature: Monitor — eye with sparkline */
        monitor: `<svg viewBox="0 0 24 24" fill="none" stroke="var(--ink-black)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/>
            <circle cx="12" cy="12" r="3"/>
            <circle cx="12" cy="12" r="1" fill="var(--ink-black)" stroke="none"/>
        </svg>`,
    };

    /**
     * Return manga-sketch SVG markup for a node type.
     * @param {string} typeId    - "device" | "edge" | "cloud"
     * @param {string} [deviceType] - for device: "camera" | "gateway" | "thermostat"
     * @returns {string} SVG markup string
     */
    function getNodeIcon(typeId, deviceType) {
        if (typeId === 'device') {
            return _icons[deviceType] || _icons.camera;
        }
        if (typeId === 'edge') return _icons.chip;
        if (typeId === 'cloud') return _icons['cloud-pc'];
        return '';
    }

    /**
     * Return manga-sketch SVG markup for a welcome-page feature card.
     * @param {string} name - "compose" | "tune" | "simulate" | "monitor"
     * @returns {string} SVG markup string
     */
    function getFeatureIcon(name) {
        return _icons[name] || '';
    }

    return { prefersReducedMotion, createHexSocket, getNodeIcon, getFeatureIcon };
})();
