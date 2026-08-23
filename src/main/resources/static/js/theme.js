// 1. APLICACIÓN DE TEMA Y CONGELACIÓN DE TRANSICIONES (Se ejecuta en <head>)
(function initThemeEngine() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);

    // Inyecta CSS temporal para bloquear animaciones durante la carga inicial del DOM
    const style = document.createElement('style');
    style.id = 'theme-transition-blocker';
    style.innerHTML = `
        .toggle, .theme-controller, .toggle * {
            transition: none !important;
        }
    `;
    document.head.appendChild(style);
})();

// 2. CAMBIO MANUAL DEL TEMA (Evento onchange del checkbox)
function toggleTheme() {
    const checkbox = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;
    
    const newTheme = checkbox.checked ? 'dark' : 'light';
    htmlElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
}

// 3. SINCRONIZACIÓN SILENCIOSA DEL CHECKBOX
function syncThemeToggleSilently() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    const checkbox = document.getElementById('theme-toggle');
    
    if (checkbox) {
        checkbox.checked = (savedTheme === 'dark');
    }

    // Una vez que el checkbox ya tiene el estado correcto en el DOM, reactivamos las transiciones
    requestAnimationFrame(() => {
        const blocker = document.getElementById('theme-transition-blocker');
        if (blocker) {
            blocker.remove();
        }
    });
}

// Listeners de sincronización
document.addEventListener("DOMContentLoaded", syncThemeToggleSilently);
document.addEventListener("htmx:afterSettle", syncThemeToggleSilently);