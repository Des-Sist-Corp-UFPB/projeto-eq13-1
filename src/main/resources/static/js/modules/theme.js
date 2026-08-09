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

    applyTheme(root.dataset.theme || 'light');

    toggle.addEventListener('click', async () => {
        const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
        applyTheme(nextTheme);
        localStorage.setItem(STORAGE_KEY, nextTheme);

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
            });
        } catch (_) {
            // A preferência local continua válida quando a rede está indisponível.
        }
    });
}
