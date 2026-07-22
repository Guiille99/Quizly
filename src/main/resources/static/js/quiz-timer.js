class QuizTimer {

    constructor() {

        this.timer = document.getElementById("quiz-timer");

        if (!this.timer) {
            return;
        }

        this.text = document.getElementById("quiz-timer-text");

        this.remaining = Number(this.timer.dataset.seconds);

        this.timeoutUrl = this.timer.dataset.timeoutUrl;

        this.finished = false;

        this.csrfToken = document
            .querySelector('meta[name="_csrf"]')
            ?.content;

        this.csrfHeader = document
            .querySelector('meta[name="_csrf_header"]')
            ?.content;

        this.render();

        this.interval = setInterval(() => this.tick(), 1000);

        document.body.addEventListener(
            "quizTimerSync",
            (event) => this.sync(event.detail.seconds)
        );
    }

    tick() {

        if (this.finished) {
            return;
        }

        this.remaining = Math.max(0, this.remaining - 1);

        this.render();

        if (this.remaining === 0) {
            this.timeout();
        }
    }

    sync(seconds) {

        if (this.finished) {
            return;
        }

        this.remaining = Math.max(0, seconds);

        this.render();

    }

    render() {

        this.text.textContent = this.format(this.remaining);

        this.updateStyle();

    }

    format(totalSeconds) {

        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;

        return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;

    }

    updateStyle() {

        this.timer.classList.remove(
            "text-success",
            "text-warning",
            "text-error",
            "animate-pulse"
        );

        if (this.remaining <= 60) {

            this.timer.classList.add(
                "text-error",
                "animate-pulse"
            );

        } else if (this.remaining <= 300) {

            this.timer.classList.add("text-warning");

        } else {

            this.timer.classList.add("text-success");

        }

    }

    timeout() {

        if (this.finished) {
            return;
        }

        this.finished = true;

        clearInterval(this.interval);

        htmx.ajax("POST", this.timeoutUrl, {
            headers: {
                [this.csrfHeader]: this.csrfToken
            }
        });

    }

}

document.addEventListener("DOMContentLoaded", () => {
    new QuizTimer();
});