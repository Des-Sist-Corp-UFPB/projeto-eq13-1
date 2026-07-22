(() => {
    const root = document.documentElement;
    const button = document.getElementById('theme-toggle');
    if (!button) return;

    const labels = { system: 'Tema automático', light: 'Tema claro', dark: 'Tema escuro' };
    const icons = { system: '◐', light: '☀', dark: '☾' };
    const sequence = ['system', 'light', 'dark'];

    const render = (theme) => {
        root.dataset.theme = theme;
        button.title = `${labels[theme]}. Clique para alterar.`;
        button.querySelector('.theme-icon').textContent = icons[theme];
    };

    render(root.dataset.theme || 'light');

    button.addEventListener('click', async () => {
        const current = root.dataset.theme || 'light';
        const theme = sequence[(sequence.indexOf(current) + 1) % sequence.length];
        render(theme);
        localStorage.setItem('radartech-theme', theme);

        if (!button.dataset.saveUrl) return;
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        if (token && header) headers[header] = token;
        try {
            await fetch(button.dataset.saveUrl, {
                method: 'POST',
                headers,
                body: new URLSearchParams({ theme })
            });
        } catch (_) {
            // A preferência local continua válida quando a rede está indisponível.
        }
    });
})();
