export function initProfileMedia() {
    document.querySelectorAll('[data-positioned-media]').forEach((media) => {
        const x = media.dataset.positionX || '50';
        const y = media.dataset.positionY || '50';
        media.style.objectPosition = `${x}% ${y}%`;
    });

    document.querySelectorAll('[data-media-dialog]').forEach((dialog) => {
        const kind = dialog.dataset.mediaDialog;
        const openButton = document.querySelector(`[data-media-open="${kind}"]`);
        const closeButton = dialog.querySelector('[data-media-close]');
        const input = dialog.querySelector('[data-media-input]');
        const preview = dialog.querySelector('[data-media-preview]');
        const xControl = dialog.querySelector('[data-position-x]');
        const yControl = dialog.querySelector('[data-position-y]');
        let previewUrl;

        if (!openButton || !preview || !xControl || !yControl) return;

        const updatePosition = () => {
            preview.style.objectPosition = `${xControl.value}% ${yControl.value}%`;
        };

        openButton.addEventListener('click', () => {
            if (typeof dialog.showModal === 'function') dialog.showModal();
            else dialog.setAttribute('open', '');
            updatePosition();
        });

        closeButton?.addEventListener('click', () => {
            if (typeof dialog.close === 'function') dialog.close();
            else dialog.removeAttribute('open');
        });

        input?.addEventListener('change', () => {
            const file = input.files && input.files[0];
            if (!file) return;
            if (previewUrl) URL.revokeObjectURL(previewUrl);
            previewUrl = URL.createObjectURL(file);
            preview.src = previewUrl;
            preview.hidden = false;
            dialog.querySelector('[data-media-empty]')?.setAttribute('hidden', '');
        });

        xControl.addEventListener('input', updatePosition);
        yControl.addEventListener('input', updatePosition);
        dialog.addEventListener('close', () => {
            if (previewUrl) URL.revokeObjectURL(previewUrl);
            previewUrl = undefined;
        });
    });
}
