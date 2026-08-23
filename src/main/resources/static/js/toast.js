const ToastSystem = (function() {
    const DURATION = 5000;

    const estilos = {
        info:    'alert-info',
        error:   'alert-error',
        success: 'alert-success',
        warning: 'alert-warning',
    };

    const icons = {
        info:    'fa-solid fa-circle-info',
        error:   'fa-solid fa-circle-xmark',
        success: 'fa-solid fa-circle-check',
        warning: 'fa-solid fa-circle-exclamation',
    }

    const obtenerContenedor = () => {
        let contenedor = document.getElementById('toast-container');
        if (!contenedor) {
            contenedor = document.createElement('div');
            contenedor.id = 'toast-container';
            contenedor.className = 'fixed top-5 right-5 z-9999 flex flex-col gap-3 pointer-events-none';
            document.body.appendChild(contenedor);
        }
        return contenedor;
    };

    const eliminar = (elemento) => {
        elemento.classList.add('opacity-0');
        setTimeout(() => elemento.remove(), 300);
    };

    const mostrar = (tipo, mensaje) => {
        if (!mensaje) return;
    
        const contenedor = obtenerContenedor();
        const template = document.getElementById("toast-template");
        const fragment = template.content.cloneNode(true);
        const toast = fragment.querySelector(".alert");
        const progressBar = fragment.querySelector(".toast-progress-bar");

        // Estilos y textos
        toast.classList.add(...(estilos[tipo] || estilos.info).split(' '));
        // fragment.querySelector(".toast-title").textContent = `${titulos[tipo]}`;
        fragment.querySelector(".toast-message").textContent = `${mensaje}`;
        fragment.querySelector(".toast-icon").classList.add(...icons[tipo].split(' '));

        // Establecer el estado inicial para el fade-in: invisible
        toast.classList.add('opacity-0');

    
        fragment.querySelector('.btn-cerrar')
            .addEventListener('click', () => eliminar(toast));
    
        contenedor.appendChild(fragment);
    
        // Animación de entrada
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                // Eliminar opacity-0 para activar la transición de fade-in a opacity-100 (estado visible por defecto).
                toast.classList.remove('opacity-0');
            });
        });

        // Animación barra de progreso
        requestAnimationFrame(() => {
            // 1. forzar estado inicial explícito
            progressBar.style.transform = "scaleX(1)";
            progressBar.style.transition = "none";

            // 2. forzar repaint
            requestAnimationFrame(() => {
                // 3. ahora sí animamos
                progressBar.style.transition = `transform ${DURATION}ms linear`;
                progressBar.style.transform = "scaleX(0)";
            });
        });
    
        setTimeout(() => {
            if (document.body.contains(toast)) {
                eliminar(toast);
            }
        }, DURATION);
    };

    return { mostrar };
})();


document.addEventListener("DOMContentLoaded", () => {
    // Listener
    document.body.addEventListener("showToast", function(evt) {
        const data = evt.detail;
        ToastSystem.mostrar(data.tipo, data.mensaje);
    });
});