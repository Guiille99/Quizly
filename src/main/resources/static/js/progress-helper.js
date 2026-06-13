/**
 * Utility to automatically assign range classes (low, medium, high)
 * to progress components based on their value.
 */
const ProgressAutoRange = (function() {
    const updateElement = (el) => {
        let val = 0;
        if (el.tagName === 'PROGRESS') {
            val = parseFloat(el.getAttribute('value')) || 0;
        } else {
            const style = el.getAttribute('style') || '';
            const match = style.match(/--value:\s*(\d+)/);
            if (match) val = parseInt(match[1]);
        }

        el.classList.remove('range-low', 'range-medium', 'range-high');
        if (val < 35) el.classList.add('range-low');
        else if (val < 70) el.classList.add('range-medium');
        else el.classList.add('range-high');
    };

    const init = () => {
        const elements = document.querySelectorAll('.auto-range');
        elements.forEach(updateElement);

        // Observe changes for dynamic updates (like HTMX or JS changes)
        const observer = new MutationObserver((mutations) => {
            mutations.forEach(m => updateElement(m.target));
        });

        elements.forEach(el => observer.observe(el, { attributes: true, attributeFilter: ['value', 'style'] }));
    };

    return { init };
})();

document.addEventListener('DOMContentLoaded', ProgressAutoRange.init);
// Soporte para HTMX
document.addEventListener('htmx:afterSwap', () => ProgressAutoRange.init());