// I-ITSM client script: live notifications over Server-Sent Events and small helpers.
(function () {
    'use strict';

    window.itsm = window.itsm || {};

    /** Headers needed for POST/PUT fetch calls (Spring Security CSRF protection). */
    itsm.csrfHeaders = function () {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        const h = {'Content-Type': 'application/json'};
        if (token && header) h[header.content] = token.content;
        return h;
    };

    itsm.escape = function (text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    };

    itsm.toast = function (message, color, link) {
        const area = document.getElementById('toastArea');
        if (!area) return;
        const el = document.createElement('div');
        el.className = 'toast align-items-center border-0 text-bg-' + (color || 'primary');
        el.setAttribute('role', 'alert');
        el.innerHTML = '<div class="d-flex"><div class="toast-body"><i class="bi bi-bell-fill me-2"></i>'
            + (link ? '<a class="text-reset" href="' + link + '">' + itsm.escape(message) + '</a>' : itsm.escape(message))
            + '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
        area.appendChild(el);
        new bootstrap.Toast(el, {delay: 7000}).show();
        el.addEventListener('hidden.bs.toast', () => el.remove());
    };

    function bumpBell() {
        const bell = document.getElementById('bellCount');
        if (!bell) return;
        bell.textContent = (parseInt(bell.textContent || '0', 10) + 1);
        bell.classList.remove('d-none');
    }

    // Live updates: only when logged in (the navbar bell exists).
    if (document.getElementById('bellCount') && window.EventSource) {
        const source = new EventSource('/live/stream');
        source.addEventListener('notification', (e) => {
            const data = JSON.parse(e.data);
            bumpBell();
            itsm.toast(data.message, data.severity, data.incidentId ? '/incidents/' + data.incidentId : null);
        });
        let timer = null;
        source.addEventListener('incident-changed', (e) => {
            // Debounce: one report triggers several events; refresh once.
            clearTimeout(timer);
            timer = setTimeout(() => document.dispatchEvent(new CustomEvent('itsm:changed', {detail: JSON.parse(e.data)})), 800);
        });
    }

    // Enable Bootstrap tooltips everywhere.
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));
})();
