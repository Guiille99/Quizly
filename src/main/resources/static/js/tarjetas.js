// Tarjetas inteligentes: mostrar respuesta, atajos de teclado, anti doble envío y confirmación de borrado.
// Todo con delegación de eventos: el fragmento de estudio se sustituye entero tras cada calificación.
(function () {
    if (window.__tarjetasInit) {
        return;
    }
    window.__tarjetasInit = true;

    const CONTENEDOR = '#tarjeta-contenido';

    function mostrarRespuesta() {
        const reverso = document.querySelector('#tarjeta-reverso');
        const calificar = document.querySelector('#tarjeta-calificar');
        const boton = document.querySelector('#btn-mostrar');
        if (!reverso || !calificar || !boton) {
            return;
        }
        reverso.classList.remove('hidden');
        calificar.classList.remove('hidden');
        boton.classList.add('hidden');
        // El foco va al reverso, no a un botón de calificar: un segundo Espacio activaría el botón
        // enfocado ("Otra vez") y suspendería la tarjeta sin querer. Además así se anuncia la respuesta.
        reverso.focus();
    }

    document.addEventListener('click', (e) => {
        if (e.target.closest('#btn-mostrar')) {
            mostrarRespuesta();
        }
    });

    // Atajos solo en la página de estudio y nunca mientras se escribe en un campo
    document.addEventListener('keydown', (e) => {
        if (!document.querySelector(CONTENEDOR) || e.ctrlKey || e.metaKey || e.altKey) {
            return;
        }
        if (e.target.closest('input, textarea, select, [contenteditable="true"]')) {
            return;
        }
        if (e.key === ' ' && document.querySelector('#btn-mostrar:not(.hidden)')) {
            e.preventDefault();
            mostrarRespuesta();
            return;
        }
        const boton = document.querySelector(`#tarjeta-calificar:not(.hidden) [data-key="${e.key}"]`);
        if (boton && !boton.disabled) {
            e.preventDefault();
            boton.click();
        }
    });

    // Un solo envío por tarjeta: los botones se deshabilitan hasta que llegue el fragmento nuevo
    document.body.addEventListener('htmx:beforeRequest', (e) => {
        if (e.target.closest('#tarjeta-calificar')) {
            document.querySelectorAll('#tarjeta-calificar button').forEach((b) => (b.disabled = true));
        }
    });

    // Tras sustituir el fragmento, el foco pasa al contenido nuevo
    document.body.addEventListener('htmx:afterSwap', (e) => {
        const nuevo = document.querySelector(CONTENEDOR);
        if (nuevo && e.detail.target && e.detail.target.id === 'tarjeta-contenido') {
            nuevo.focus();
        }
    });

    // Editor de mazos: un solo formulario, así que se avisa si se sale con cambios sin guardar
    let sinGuardar = false;
    document.addEventListener('input', (e) => {
        if (e.target.closest('#mazo-form')) {
            sinGuardar = true;
        }
    });
    document.addEventListener('submit', () => (sinGuardar = false));
    window.addEventListener('beforeunload', (e) => {
        if (sinGuardar) {
            e.preventDefault();
        }
    });

    // Borrados con confirmación (botones con data-confirm dentro de un formulario)
    document.addEventListener('click', async (e) => {
        const boton = e.target.closest('button[data-confirm]');
        if (!boton || boton.dataset.confirmado === '1') {
            return;
        }
        e.preventDefault();
        // confirmDialog es un `let` global de confirm-dialog.js (no cuelga de window)
        if (typeof confirmDialog === 'undefined') {
            return;
        }
        const resultado = await confirmDialog.open({
            title: '¿Eliminar?',
            message: boton.dataset.confirm,
            confirmBtnText: 'Eliminar',
            type: 'danger',
            icon: 'fa-regular fa-trash-can',
        });
        if (resultado.isConfirmed) {
            boton.dataset.confirmado = '1';
            boton.form.requestSubmit(boton);
        }
    });
})();
