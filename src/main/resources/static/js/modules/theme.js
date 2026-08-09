const STORAGE_KEY = 'radartech-theme';

export function initTheme() {
    const root = document.documentElement;
    const toggle = document.getElementById('theme-toggle');
    if (!toggle) return;

    const applyTheme = (theme) => {
        const currentTheme = theme === 'dark' ? 'dark' : 'light';
        const nextLabel = currentTheme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro';
        root.dataset.theme = currentTheme;
        toggle.title = nextLabel;
        toggle.setAttribute('aria-label', nextLabel);
        toggle.querySelector('.sr-only').textContent = nextLabel;
        document.querySelector('meta[name="theme-color"]')
            ?.setAttribute('content', currentTheme === 'dark' ? '#08111F' : '#FFFFFF');
    };

    const storedTheme = readStoredTheme();
    applyTheme(storedTheme || root.dataset.theme || 'light');

    toggle.addEventListener('click', async () => {
        const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
        applyTheme(nextTheme);
        storeTheme(nextTheme);

        const saveUrl = toggle.dataset.saveUrl;
        if (!saveUrl) return;
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        if (token && header) headers[header] = token;
        try {
            await fetch(saveUrl, {
                method: 'POST',
                headers,
                body: new URLSearchParams({ theme: nextTheme }),
                keepalive: true,
            });
        } catch (_) {
            // A preferência local continua válida quando a rede está indisponível.
        }
    });

    window.addEventListener('storage', (event) => {
        if (event.key === STORAGE_KEY && (event.newValue === 'dark' || event.newValue === 'light')) {
            applyTheme(event.newValue);
        }
    });

    window.addEventListener('pageshow', () => {
        const savedTheme = readStoredTheme();
        if (savedTheme) applyTheme(savedTheme);
    });
}

function readStoredTheme() {
    try {
        const value = localStorage.getItem(STORAGE_KEY);
        return value === 'dark' || value === 'light' ? value : null;
    } catch (_) {
        return null;
    }
}

function storeTheme(theme) {
    try {
        localStorage.setItem(STORAGE_KEY, theme);
    } catch (_) {
        // O tema ainda permanece aplicado durante a navegação atual.
    }
}
