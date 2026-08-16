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

    return { prefersReducedMotion, createHexSocket };
})();
