const STORAGE_KEY = 'radartech-theme';
const LABELS = { system: 'Tema automático', light: 'Tema claro', dark: 'Tema escuro' };

export function initTheme() {
    const root = document.documentElement;
    const menu = document.getElementById('theme-menu');
    const toggle = document.getElementById('theme-toggle');
    const panel = document.getElementById('theme-menu-panel');
    if (!menu || !toggle || !panel) return;

    const applyTheme = (theme) => {
        root.dataset.theme = theme;
        toggle.title = `${LABELS[theme] ?? 'Tema'}. Clique para alterar.`;
        panel.querySelectorAll('[data-theme-choice]').forEach((item) => {
            item.setAttribute('aria-current', item.dataset.themeChoice === theme ? 'true' : 'false');
        });
    };

    applyTheme(root.dataset.theme || 'light');

    const openPanel = () => {
        panel.dataset.open = 'true';
        toggle.setAttribute('aria-expanded', 'true');
    };

    const closePanel = () => {
        panel.dataset.open = 'false';
        toggle.setAttribute('aria-expanded', 'false');
    };

    toggle.addEventListener('click', (event) => {
        event.stopPropagation();
        if (panel.dataset.open === 'true') {
            closePanel();
        } else {
            openPanel();
        }
    });

    document.addEventListener('click', (event) => {
        if (!menu.contains(event.target)) closePanel();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closePanel();
            toggle.focus();
        }
    });

    panel.querySelectorAll('[data-theme-choice]').forEach((item) => {
        item.addEventListener('click', async () => {
            const theme = item.dataset.themeChoice;
            applyTheme(theme);
            closePanel();
            localStorage.setItem(STORAGE_KEY, theme);

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
                    body: new URLSearchParams({ theme }),
                });
            } catch (_) {
                // A preferência local continua válida quando a rede está indisponível.
            }
        });
    });
}
