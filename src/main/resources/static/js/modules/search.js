// Melhorias de acessibilidade e usabilidade para barras de busca reais
// (que enviam GET para o back-end). Nunca filtra dados localmente.
export function initSearch() {
    document.querySelectorAll('[data-search-clear]').forEach((button) => {
        const input = document.querySelector(button.getAttribute('data-search-clear'));
        if (!input) return;
        const toggle = () => {
            button.hidden = input.value.length === 0;
        };
        toggle();
        input.addEventListener('input', toggle);
        button.addEventListener('click', () => {
            input.value = '';
            input.focus();
            toggle();
        });
    });
}
