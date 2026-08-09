// Melhorias visuais para ações administrativas. Nenhuma chamada é
// feita para endpoints além dos formulários POST já existentes; as
// confirmações destrutivas em si são tratadas em modules/forms.js
// através de [data-confirm], reaproveitado também na área admin.
export function initAdmin() {
    const table = document.querySelector('[data-admin-table]');
    if (!table) return;

    table.querySelectorAll('tbody tr').forEach((row) => {
        row.addEventListener('click', (event) => {
            if (event.target.closest('a, button, form')) return;
            const link = row.querySelector('[data-row-link]');
            if (link) link.click();
        });
    });
}
