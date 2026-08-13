document.addEventListener('DOMContentLoaded', () => {

    document.querySelectorAll('[data-toggle-disabled]').forEach(toggle => {

        const selector = toggle.dataset.toggleDisabled;
        const targets = document.querySelectorAll(selector);

        if (!targets.length) {
            return;
        }

        const readOnlyElements = Array.from(targets).filter(target => target.disabled);

        const updateState = () => {
            targets.forEach(target => {
                // Si el elemento ya estaba deshabilitado de origen, no modificamos
                if (readOnlyElements.includes(target)) {
                    return;
                }
                target.disabled = toggle.checked;
            });
        };

        toggle.addEventListener('change', updateState);

        // Inicializar el estado al cargar la página
        updateState();
    });

});