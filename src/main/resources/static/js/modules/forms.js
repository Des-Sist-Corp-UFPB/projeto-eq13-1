export function initForms() {
    initLoadingSubmit();
    initFileNamePreview();
    initDestructiveConfirm();
    initCityHint();
}

// Aplica estado de carregamento e impede duplo clique em qualquer
// formulário marcado com a classe "js-loading-submit". Não interfere
// na validação nativa/server-side.
function initLoadingSubmit() {
    document.querySelectorAll('form.js-loading-submit').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (form.dataset.submitted === 'true') {
                event.preventDefault();
                return;
            }
            if (!form.checkValidity()) return;
            form.dataset.submitted = 'true';
            form.querySelectorAll('button[type="submit"]').forEach((button) => {
                button.classList.add('btn-loading');
                button.disabled = true;
            });
        });
    });
}

// Mostra o nome do arquivo selecionado em campos de upload
// (input[type="file"] dentro de um elemento com [data-file-field]).
function initFileNamePreview() {
    document.querySelectorAll('[data-file-field]').forEach((field) => {
        const input = field.querySelector('input[type="file"]');
        const label = field.querySelector('[data-file-name]');
        if (!input || !label) return;
        const defaultText = label.textContent;
        input.addEventListener('change', () => {
            const file = input.files && input.files[0];
            label.textContent = file ? file.name : defaultText;
        });
    });
}

// Pede confirmação antes de enviar ações destrutivas
// (formulários/botões marcados com [data-confirm="mensagem"]).
function initDestructiveConfirm() {
    document.querySelectorAll('[data-confirm]').forEach((element) => {
        element.addEventListener('submit', (event) => {
            const message = element.getAttribute('data-confirm');
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
        if (element.tagName === 'BUTTON') {
            element.addEventListener('click', (event) => {
                const message = element.getAttribute('data-confirm');
                if (message && !window.confirm(message)) {
                    event.preventDefault();
                    event.stopPropagation();
                }
            });
        }
    });
}

// Auxílio visual (não normativo) para indicar quando o campo de
// cidade costuma ser necessário conforme o modelo de trabalho.
// A validação final continua sendo feita pelo back-end.
function initCityHint() {
    const locationField = document.querySelector('[data-location-field]');
    const cityField = document.querySelector('[data-city-field]');
    if (!locationField || !cityField) return;

    const update = () => {
        const isRemote = locationField.value === 'REMOTE';
        cityField.classList.toggle('is-optional-hint', isRemote);
        const hint = cityField.querySelector('[data-city-hint]');
        if (hint) {
            hint.textContent = isRemote
                ? 'Opcional para vagas remotas.'
                : 'Obrigatório para vagas híbridas ou presenciais.';
        }
    };

    locationField.addEventListener('change', update);
    update();
}
