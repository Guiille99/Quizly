document.addEventListener('DOMContentLoaded', () => {

    document.querySelectorAll('[data-toggle-disabled]').forEach(toggle => {

        const selector = toggle.dataset.toggleDisabled;
        const targets = document.querySelectorAll(selector);

        if (!targets.length) {
            return;
        }

        const updateState = () => {
            targets.forEach(target => {
                target.disabled = toggle.checked;
            });
        };

        toggle.addEventListener('change', updateState);

        // Inicializar el estado al cargar la página
        updateState();
    });

});