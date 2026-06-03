const ToastSystem = (function() {
    const DURATION = 5000;

    const estilos = {
        info:    'text-blue-800 bg-blue-50 border-blue-300',
        error:   'text-red-800 bg-red-50 border-red-200',
        success: 'text-emerald-900 bg-emerald-50 border-emerald-300',
        warning: 'text-amber-800 bg-amber-50 border-amber-300',
    };

    const progressColors = {
        success: "bg-emerald-500",
        error: "bg-red-500",
        warning: "bg-amber-500",
        info: "bg-blue-500",
    };
    
    const titulos = {
        info:    'Información',
        error:   'Error',
        success: 'Éxito',
        warning: 'Atención',
    };

    const icons = {
        info:    'bi bi-info-circle',
        error:   'bi bi-x-circle',
        success: 'bi bi-check-circle',
        warning: 'bi bi-exclamation-circle',
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
        elemento.classList.add('opacity-0', 'translate-x-full');
        setTimeout(() => elemento.remove(), 300);
    };

    const mostrar = (tipo, mensaje) => {
        if (!mensaje) return;
    
        const contenedor = obtenerContenedor();
        const template = document.getElementById("toast-template");
        const fragment = template.content.cloneNode(true);
        const toast = fragment.querySelector(".toast");
        const progressBar = fragment.querySelector(".toast-progress-bar");

        // Estilos y textos
        toast.classList.add(...(estilos[tipo] || estilos.info).split(' '));
        // fragment.querySelector(".toast-title").textContent = `${titulos[tipo]}`;
        fragment.querySelector(".toast-message").textContent = `${mensaje}`;
        fragment.querySelector(".toast-icon").classList.add(...icons[tipo].split(' '));
        progressBar.classList.add(progressColors[tipo]);

    
        fragment.querySelector('.btn-cerrar')
            .addEventListener('click', () => eliminar(toast));
    
        contenedor.appendChild(fragment);
    
        // Animación de entrada
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                toast.classList.remove('translate-x-full', 'opacity-0');
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
    
        // setTimeout(() => {
        //     if (document.body.contains(toast)) {
        //         eliminar(toast);
        //     }
        // }, DURATION);
    };

    return { mostrar };
})();