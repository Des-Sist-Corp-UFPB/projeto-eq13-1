export function initNavigation() {
    initScrollShadow();
    initMobileMenu();
    initProfileDropdown();
    initAdminNavActiveState();
}

function initAdminNavActiveState() {
    const nav = document.querySelector('.admin-nav');
    if (!nav) return;
    const current = window.location.pathname;
    let bestMatch = null;
    nav.querySelectorAll('a').forEach((link) => {
        const path = new URL(link.href, window.location.origin).pathname;
        if (current === path || (path !== '/admin' && current.startsWith(path))) {
            if (!bestMatch || path.length > bestMatch.length) bestMatch = path;
        }
    });
    nav.querySelectorAll('a').forEach((link) => {
        const path = new URL(link.href, window.location.origin).pathname;
        if (path === bestMatch || (bestMatch === null && path === '/admin' && current === '/admin')) {
            link.setAttribute('aria-current', 'page');
        }
    });
}

function initScrollShadow() {
    const header = document.getElementById('site-header');
    if (!header) return;
    const onScroll = () => {
        header.classList.toggle('is-scrolled', window.scrollY > 4);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
}

function initMobileMenu() {
    const hamburger = document.getElementById('nav-hamburger');
    const links = document.getElementById('site-nav-links');
    const scrim = document.getElementById('nav-scrim');
    if (!hamburger || !links) return;

    const close = () => {
        links.dataset.open = 'false';
        hamburger.setAttribute('aria-expanded', 'false');
        document.body.dataset.navOpen = 'false';
        if (scrim) scrim.dataset.open = 'false';
    };

    const open = () => {
        links.dataset.open = 'true';
        hamburger.setAttribute('aria-expanded', 'true');
        document.body.dataset.navOpen = 'true';
        if (scrim) scrim.dataset.open = 'true';
    };

    hamburger.addEventListener('click', () => {
        if (links.dataset.open === 'true') close(); else open();
    });

    if (scrim) scrim.addEventListener('click', close);

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && links.dataset.open === 'true') {
            close();
            hamburger.focus();
        }
    });

    links.querySelectorAll('a').forEach((link) => link.addEventListener('click', close));

    window.addEventListener('resize', () => {
        if (window.innerWidth >= 1024) close();
    });
}

function initProfileDropdown() {
    const menu = document.getElementById('profile-menu');
    if (!menu) return;
    const toggle = menu.querySelector('.nav-profile');
    const panel = menu.querySelector('.dropdown-panel');
    if (!toggle || !panel) return;

    const close = () => {
        panel.dataset.open = 'false';
        toggle.setAttribute('aria-expanded', 'false');
    };

    const open = () => {
        panel.dataset.open = 'true';
        toggle.setAttribute('aria-expanded', 'true');
    };

    toggle.addEventListener('click', (event) => {
        event.stopPropagation();
        if (panel.dataset.open === 'true') close(); else open();
    });

    document.addEventListener('click', (event) => {
        if (!menu.contains(event.target)) close();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            close();
            toggle.focus();
        }
    });
}
