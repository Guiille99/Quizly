const PageAnimations = (() => {

    function animateElements(root = document) {

        root.querySelectorAll(".animate-on-load:not(.is-visible)")
            .forEach((element, index) => {

                setTimeout(() => {
                    element.classList.add("is-visible");
                }, index * 60);

            });

    }

    function animateRadials(root = document) {

        root.querySelectorAll(".radial-progress[data-value]")
            .forEach(radial => {

                // Evitar animarla varias veces
                if (radial.dataset.animated === "true") {
                    return;
                }

                radial.dataset.animated = "true";

                radial.style.setProperty("--value", 0);

                requestAnimationFrame(() => {

                    radial.style.setProperty(
                        "--value",
                        radial.dataset.value
                    );

                });

            });

    }

    function init(root = document) {
        animateElements(root);
        animateRadials(root);
    }

    return {
        init
    };

})();

document.addEventListener("DOMContentLoaded", () => {
    PageAnimations.init();
});

document.addEventListener("htmx:afterSwap", (event) => {
    PageAnimations.init(event.target);
});