function toggleTheme() {
    const checkbox = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;
    
    if (checkbox.checked) {
        htmlElement.setAttribute('data-theme', 'dark');
        localStorage.setItem('theme', 'dark');
    } else {
        htmlElement.setAttribute('data-theme', 'light');
        localStorage.setItem('theme', 'light');
    }
}

// AL CARGAR LA PÁGINA / CAMBIAR DE PESTAÑA: Sincronizar tema y checkbox
document.addEventListener("DOMContentLoaded", () => {
    const savedTheme = localStorage.getItem('theme') || 'light';
    const checkbox = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;

    // 1. Aplicar el tema guardado al HTML
    htmlElement.setAttribute('data-theme', savedTheme);

    // 2. Sincronizar el estado visual del checkbox
    if (checkbox) {
        checkbox.checked = (savedTheme === 'dark');
    }
});