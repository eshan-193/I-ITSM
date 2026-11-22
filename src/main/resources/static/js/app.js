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

    // Several pages reload themselves when an incident changes, which would wipe a toast that
    // has only just appeared. Live toasts are therefore remembered for a few seconds in
    // sessionStorage and shown again after such a reload.
    const PENDING_KEY = 'itsm.pendingToasts';
    const PENDING_MS = 8000;

    function readPending() {
        try {
            return JSON.parse(sessionStorage.getItem(PENDING_KEY) || '[]').filter(t => Date.now() - t.at < PENDING_MS);
        } catch (e) {
            return [];
        }
    }

    function writePending(list) {
        try {
            sessionStorage.setItem(PENDING_KEY, JSON.stringify(list));
        } catch (e) { /* storage unavailable: the toast simply is not carried over */ }
    }

    itsm.toast = function (message, color, link, pendingId) {
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
        el.addEventListener('hidden.bs.toast', () => {
            el.remove();
            if (pendingId) writePending(readPending().filter(t => t.id !== pendingId));
        });
    };

    function bumpBell() {
        const bell = document.getElementById('bellCount');
        if (!bell) return;
        bell.textContent = (parseInt(bell.textContent || '0', 10) + 1);
        bell.classList.remove('d-none');
    }

    // Live updates: only when logged in (the navbar bell exists).
    if (document.getElementById('bellCount') && window.EventSource) {
        readPending().forEach(t => itsm.toast(t.message, t.color, t.link, t.id));

        const source = new EventSource('/live/stream');
        source.addEventListener('notification', (e) => {
            const data = JSON.parse(e.data);
            const toast = {
                id: Date.now() + '-' + Math.random(), at: Date.now(), message: data.message, color: data.severity,
                link: data.incidentId ? '/incidents/' + data.incidentId : null
            };
            writePending(readPending().concat(toast));
            bumpBell();
            itsm.toast(toast.message, toast.color, toast.link, toast.id);
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
