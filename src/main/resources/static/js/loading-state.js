document.addEventListener("submit", (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) {
        return;
    }

    const button = event.submitter || form.querySelector("button[data-loading-text]");
    if (!(button instanceof HTMLButtonElement) || !button.dataset.loadingText) {
        return;
    }

    if (form.dataset.submitting === "true") {
        event.preventDefault();
        return;
    }

    form.dataset.submitting = "true";
    form.setAttribute("aria-busy", "true");
    button.dataset.loadingOriginal = button.innerHTML;
    button.dataset.loadingOriginalLive = button.getAttribute("aria-live") || "";
    button.style.minWidth = `${button.getBoundingClientRect().width}px`;
    button.disabled = true;
    button.setAttribute("aria-live", "polite");

    const spinner = document.createElement("span");
    spinner.className = "loading loading-spinner loading-sm";
    spinner.setAttribute("aria-hidden", "true");

    const label = document.createElement("span");
    label.textContent = button.dataset.loadingText;

    button.replaceChildren(spinner, label);
});

window.addEventListener("pageshow", () => {
    document.querySelectorAll('form[data-submitting="true"]').forEach((form) => {
        form.removeAttribute("aria-busy");
        delete form.dataset.submitting;

        const button = form.querySelector("button[data-loading-text]");
        if (!button) {
            return;
        }

        button.disabled = false;
        button.style.removeProperty("min-width");
        button.innerHTML = button.dataset.loadingOriginal;
        if (button.dataset.loadingOriginalLive) {
            button.setAttribute("aria-live", button.dataset.loadingOriginalLive);
        } else {
            button.removeAttribute("aria-live");
        }
        delete button.dataset.loadingOriginal;
        delete button.dataset.loadingOriginalLive;
    });
});