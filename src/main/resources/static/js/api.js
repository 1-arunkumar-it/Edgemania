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
        const json = await res.json();
        if (!res.ok) throw new Error(json.error || `HTTP ${res.status}`);
        return json;
    }
    return {
        get:    (path) => request('GET', path),
        post:   (path, body) => request('POST', path, body),
        put:    (path, body) => request('PUT', path, body),
        del:    (path) => request('DELETE', path),
    };
})();
