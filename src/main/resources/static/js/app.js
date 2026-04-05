/* =============================================
   LuxeStay — app.js
   ============================================= */

// ── Token Management ────────────────────────────
function saveTokens(accessToken, refreshToken, user) {
    if (accessToken) localStorage.setItem('access_token', accessToken);
    if (refreshToken) localStorage.setItem('refresh_token', refreshToken);
    if (user) localStorage.setItem('user', JSON.stringify(user));
}
function getAccessToken() { return localStorage.getItem('access_token'); }
function getRefreshToken() { return localStorage.getItem('refresh_token'); }
function getUser() { try { return JSON.parse(localStorage.getItem('user')); } catch { return null; } }
function clearTokens() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
}
function requireAuth() {
    if (!getAccessToken()) {
        window.location.href = '/auth/login?redirect=' + encodeURIComponent(window.location.pathname);
    }
}
function requireAdmin() {
    const token = getAccessToken();
    const user = getUser();
    if (!token || !user) {
        window.location.href = '/auth/login?redirect=' + encodeURIComponent(window.location.pathname);
        return;
    }
    if (user.role !== 'ADMIN' && user.role !== 'STAFF') {
        window.location.href = '/';
    }
}

// ── API Helpers ──────────────────────────────────
async function apiFetch(url, options = {}) {
    const token = getAccessToken();
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    let res = await fetch(url, { ...options, headers });

    if (res.status === 401 && getRefreshToken()) {
        const refreshed = await tryRefresh();
        if (refreshed) {
            headers['Authorization'] = 'Bearer ' + getAccessToken();
            res = await fetch(url, { ...options, headers });
        } else {
            clearTokens();
            window.location.href = '/auth/login';
            return;
        }
    }

    if (res.status === 403) {
        const err = new Error('You do not have permission to access this resource');
        err.status = 403;
        throw err;
    }

    const contentType = res.headers.get('content-type') || '';
    if (!contentType.includes('application/json')) {
        throw new Error('Server returned an unexpected response. Please try again.');
    }

    const data = await res.json();
    if (!res.ok) {
        const err = new Error(data.message || 'Request failed');
        err.data = data.data;
        err.status = res.status;
        throw err;
    }
    return data;
}

async function apiPost(url, body) { return apiFetch(url, { method: 'POST', body: JSON.stringify(body) }); }
async function apiPut(url, body) { return apiFetch(url, { method: 'PUT', body: JSON.stringify(body) }); }
async function apiDelete(url) { return apiFetch(url, { method: 'DELETE' }); }

async function tryRefresh() {
    const rt = getRefreshToken();
    if (!rt) return false;
    try {
        const res = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: rt })
        });
        if (!res.ok) return false;
        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) return false;
        const data = await res.json();
        if (data.data?.accessToken) {
            saveTokens(data.data.accessToken, data.data.refreshToken, data.data.user);
            return true;
        }
        return false;
    } catch { return false; }
}

async function logout() {
    try { await apiPost('/api/auth/logout', { refreshToken: getRefreshToken() }); } catch {}
    clearTokens();
    window.location.href = '/auth/login';
}

// ── Navbar ───────────────────────────────────────
function initNavbar() {
    const token = getAccessToken();
    const user = getUser();
    const navGuest = document.getElementById('navGuest');
    const navUser = document.getElementById('navUser');
    if (navGuest) navGuest.style.display = token ? 'none' : 'flex';
    if (navUser) navUser.style.display = token ? 'flex' : 'none';
    if (token && user) {
        const el = document.getElementById('navUsername');
        if (el) el.textContent = (user.name || user.username || 'U').charAt(0).toUpperCase();
        document.querySelectorAll('.nav-auth-only').forEach(el => el.style.display = 'block');
        if (user.role === 'ADMIN' || user.role === 'STAFF') {
            document.querySelectorAll('.nav-admin-only').forEach(el => el.style.display = 'block');
        }
        // Show bell only for guests
        const bell = document.getElementById('notifBell');
        if (bell) {
            bell.style.display = user.role === 'GUEST' ? 'flex' : 'none';
        }
    }
}

// ── Notification System ──────────────────────────
const notifStore = {
    items: JSON.parse(localStorage.getItem('notifications') || '[]'),
    unreadCount: parseInt(localStorage.getItem('notif_unread') || '0'),

    add(notif) {
        notif.id = Date.now();
        notif.read = false;
        notif.time = new Date().toISOString();
        this.items.unshift(notif);
        if (this.items.length > 50) this.items = this.items.slice(0, 50);
        this.unreadCount++;
        this.save();
    },
    markAllRead() {
        this.items.forEach(n => n.read = true);
        this.unreadCount = 0;
        this.save();
    },
    remove(id) {
        this.items = this.items.filter(n => n.id !== id);
        this.save();
        // Keep dropdown open after removing item
        const dropdown = document.getElementById('notifDropdown');
        if (dropdown) dropdown.classList.add('notif-open');
    },
    save() {
        localStorage.setItem('notifications', JSON.stringify(this.items));
        localStorage.setItem('notif_unread', this.unreadCount);
        updateNotifBadge();
        renderNotifDropdown();
    }
};

function updateNotifBadge() {
    const badge = document.getElementById('notifBadge');
    if (!badge) return;
    if (notifStore.unreadCount > 0) {
        badge.style.display = 'flex';
        badge.textContent = notifStore.unreadCount > 9 ? '9+' : notifStore.unreadCount;
    } else {
        badge.style.display = 'none';
    }
}

function renderNotifDropdown() {
    const dropdown = document.getElementById('notifDropdown');
    if (!dropdown) return;

    if (!notifStore.items.length) {
        dropdown.innerHTML = '<div class="notif-empty">No notifications</div>';
        return;
    }

    dropdown.innerHTML = `
        <div class="notif-header">
            <span>Notifications</span>
            <button class="notif-mark-read" onclick="notifStore.markAllRead()">Mark all read</button>
        </div>
        ${notifStore.items.map(n => `
            <div class="notif-item ${n.read ? '' : 'unread'} notif-type-${getNotifType(n.type)}">
                <div class="notif-item-icon">${getNotifIcon(n.type)}</div>
                <div class="notif-item-body">
                    <div class="notif-msg">${n.message || n.type}</div>
                    <div class="notif-time">${formatNotifTime(n.time)}</div>
                </div>
                <button class="notif-close" onclick="notifStore.remove(${n.id})">×</button>
            </div>`).join('')}`;
}

function getNotifType(type) {
    if (!type) return 'info';
    type = type.toUpperCase();
    if (type.includes('SUCCESS') || type.includes('CONFIRMED') || type.includes('CHECKED_IN')) return 'success';
    if (type.includes('FAILED') || type.includes('CANCELLED')) return 'error';
    if (type.includes('PAYMENT') || type.includes('BOOKING')) return 'warning';
    return 'info';
}

function getNotifIcon(type) {
    if (!type) return '🔔';
    type = type.toUpperCase();
    if (type.includes('PAYMENT_SUCCESS') || type.includes('CONFIRMED')) return '✅';
    if (type.includes('PAYMENT_FAILED') || type.includes('FAILED')) return '❌';
    if (type.includes('CANCELLED')) return '🚫';
    if (type.includes('CHECKED_IN')) return '🏨';
    if (type.includes('CHECKED_OUT')) return '👋';
    if (type.includes('BOOKING')) return '📅';
    if (type.includes('PAYMENT')) return '💳';
    return '🔔';
}

function formatNotifTime(isoTime) {
    if (!isoTime) return '';
    const diff = Date.now() - new Date(isoTime).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    return new Date(isoTime).toLocaleDateString();
}

// Open/close dropdown and mark as read
document.addEventListener('click', (e) => {
    const bell = document.getElementById('notifBell');
    const dropdown = document.getElementById('notifDropdown');
    if (!bell || !dropdown) return;

    // Clicked the × close button — remove item but keep dropdown open
    if (e.target.classList.contains('notif-close')) {
        e.stopPropagation();
        return;
    }

    if (bell === e.target || bell.contains(e.target)) {
        const isOpen = dropdown.classList.contains('notif-open');
        dropdown.classList.toggle('notif-open', !isOpen);
        if (!isOpen) notifStore.markAllRead();
    } else if (!dropdown.contains(e.target)) {
        dropdown.classList.remove('notif-open');
    }
});

function addNotification(notif) {
    notifStore.add(notif);
    showToast(notif.message || 'New notification', getNotifType(notif.type));
}

// ── WebSocket ────────────────────────────────────
let stompClient = null;

function initWebSocket() {
    const token = getAccessToken();
    const user = getUser();
    if (!token || !user) return;

    const tryConnect = () => {
        if (typeof window.SockJS === 'undefined' || typeof window.Stomp === 'undefined') {
            setTimeout(tryConnect, 500);
            return;
        }
        try {
            const socket = new window.SockJS('/ws');
            stompClient = window.Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, () => {
                console.log('WebSocket connected!');

                // Only GUESTS get personal notifications
                if (user.role === 'GUEST') {
                    stompClient.subscribe('/topic/user.' + user.username, (msg) => {
                        const notif = JSON.parse(msg.body);
                        addNotification(notif);
                        handleRealtimeUpdate(notif);
                    });
                    console.log('Subscribed to: /topic/user.' + user.username);
                }

                // Everyone subscribes to booking updates for real-time refresh
                stompClient.subscribe('/topic/bookings', (msg) => {
                    console.log('Booking update received');
                    if (typeof loadBookings === 'function') setTimeout(loadBookings, 300);
                    if (typeof loadStats === 'function') setTimeout(loadStats, 300);
                });

                // Room updates
                stompClient.subscribe('/topic/rooms', () => {
                    if (typeof loadRooms === 'function') loadRooms();
                    if (typeof searchRooms === 'function') searchRooms(0);
                });

            }, (error) => {
                setTimeout(tryConnect, 5000);
            });
        } catch (e) {
            setTimeout(tryConnect, 5000);
        }
    };
    tryConnect();
}

// Real-time data refresh based on notification type
function handleRealtimeUpdate(notif) {
    if (!notif.type) return;
    const type = notif.type.toUpperCase();

    // Refresh bookings list if on bookings page
    if (type.includes('BOOKING') || type.includes('CHECKED')) {
        if (typeof loadBookings === 'function') setTimeout(loadBookings, 500);
        if (typeof loadStats === 'function') setTimeout(loadStats, 500);
    }
    // Refresh stats on payment
    if (type.includes('PAYMENT')) {
        if (typeof loadStats === 'function') setTimeout(loadStats, 500);
        if (typeof loadBookings === 'function') setTimeout(loadBookings, 500);
    }
}

// Auto-poll stats every 30 seconds (professional approach)
function startStatsPoll(loadFn) {
    loadFn();
    setInterval(loadFn, 30000);
}

// ── Toast ────────────────────────────────────────
function showToast(message, type = 'info', duration = 5000) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || icons.info}</span>
        <span class="toast-text">${message}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">×</button>`;
    container.appendChild(toast);
    // Animate in
    requestAnimationFrame(() => toast.classList.add('toast-show'));
    // Auto dismiss
    setTimeout(() => {
        toast.classList.remove('toast-show');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// ── Field Errors ─────────────────────────────────
function showFieldError(fieldId, message) {
    const el = document.getElementById(fieldId);
    if (el) el.textContent = message;
}
function clearErrors() {
    document.querySelectorAll('.field-error').forEach(el => el.textContent = '');
}
function renderFieldErrors(errors) {
    if (!errors) return;
    Object.entries(errors).forEach(([field, msg]) => showFieldError(field + 'Error', msg));
}

// ── Button Loading ────────────────────────────────
function setButtonLoading(btn, loading) {
    const text = btn.querySelector('.btn-text');
    const spinner = btn.querySelector('.btn-spinner');
    btn.disabled = loading;
    if (text) text.style.display = loading ? 'none' : 'inline-flex';
    if (spinner) spinner.style.display = loading ? 'inline-flex' : 'none';
}

// ── Modal ─────────────────────────────────────────
function showConfirmModal(title, message, onConfirm) {
    const modal = document.getElementById('confirmModal');
    if (!modal) return;
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalMessage').textContent = message;
    document.getElementById('modalConfirmBtn').onclick = () => { closeModal(); onConfirm(); };
    modal.style.display = 'flex';
}
function closeModal() {
    const modal = document.getElementById('confirmModal');
    if (modal) modal.style.display = 'none';
}

// ── Password ──────────────────────────────────────
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.type = input.type === 'password' ? 'text' : 'password';
}
function updatePasswordStrength(password) {
    const bar = document.getElementById('pwStrength');
    if (!bar) return;
    bar.className = 'password-strength';
    if (!password) return;
    const strong = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/.test(password);
    const medium = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(password);
    if (strong) bar.classList.add('pw-strong');
    else if (medium) bar.classList.add('pw-medium');
    else bar.classList.add('pw-weak');
}

// ── Pagination ────────────────────────────────────
function renderPagination(pageData, containerId, loadFn) {
    const container = document.getElementById(containerId);
    if (!container || pageData.totalPages <= 1) { if (container) container.innerHTML = ''; return; }
    let html = '';
    if (!pageData.first) html += `<button class="page-btn" onclick="${loadFn.name}(${pageData.page - 1})">‹ Prev</button>`;
    const start = Math.max(0, pageData.page - 2);
    const end = Math.min(pageData.totalPages - 1, pageData.page + 2);
    for (let i = start; i <= end; i++) {
        html += `<button class="page-btn ${i === pageData.page ? 'active' : ''}" onclick="${loadFn.name}(${i})">${i + 1}</button>`;
    }
    if (!pageData.last) html += `<button class="page-btn" onclick="${loadFn.name}(${pageData.page + 1})">Next ›</button>`;
    container.innerHTML = html;
}

// ── Init on load ──────────────────────────────────
window.addEventListener('DOMContentLoaded', () => {
    updateNotifBadge();
    renderNotifDropdown();
});

// ── Load WS libs ─────────────────────────────────
(function loadWSLibs() {
    if (typeof window.SockJS === 'undefined') {
        const s = document.createElement('script');
        s.src = 'https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js';
        document.head.appendChild(s);
    }
    if (typeof window.Stomp === 'undefined') {
        const s = document.createElement('script');
        s.src = 'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';
        document.head.appendChild(s);
    }
})();