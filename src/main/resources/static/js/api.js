/* EdgeMania.io — api.js  Thin fetch wrapper (JSON in/out). */
const api = (() => {
    async function request(method, path, body) {
        const opts = { method, headers: {} };
        if (body !== undefined) {
            opts.headers['Content-Type'] = 'application/json';
            opts.body = JSON.stringify(body);
        }
        const res = await fetch(path, opts);
        if (res.status === 204) return null;
        let json;
        try {
            json = await res.json();
        } catch {
            json = { status: res.status, error: 'HTTP ' + res.status, message: res.statusText || 'Request failed' };
        }
        if (!res.ok) throw new Error(json.error || 'HTTP ' + res.status);
        return json;
    }
    return {
        get:    (path) => request('GET', path),
        post:   (path, body) => request('POST', path, body),
        put:    (path, body) => request('PUT', path, body),
        del:    (path) => request('DELETE', path),
    };
})();

/* Show a themed error banner. Auto-dismisses; user can also close it. */
function showErrorBanner(msg) {
    let banner = document.getElementById('error-banner');
    if (!banner) {
        banner = document.createElement('div');
        banner.id = 'error-banner';
        banner.className = 'error-banner';
        banner.setAttribute('role', 'alert');
        banner.innerHTML =
            '<span class="error-banner__msg"></span>' +
            '<button class="error-banner__dismiss" aria-label="Dismiss">dismiss</button>';
        document.body.appendChild(banner);
        banner.querySelector('.error-banner__dismiss').addEventListener('click', () => banner.remove());
    }
    banner.querySelector('.error-banner__msg').textContent = msg;
    clearTimeout(banner._timer);
    banner._timer = setTimeout(() => banner.remove(), 10000);
}
