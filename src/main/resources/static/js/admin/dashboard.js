const centerTextPlugin = {

    id: "centerText",

    beforeDraw(chart, args, options) {

        const { ctx } = chart;
        const { width, height } = chart;

        ctx.save();

        const centerX = width / 2;
        const centerY = height / 2;

        // Valor
        ctx.font = "bold 74px Sora";
        ctx.fillStyle = "#1f2937";
        ctx.textAlign = "center";
        ctx.textBaseline = "center";

        ctx.fillText(options.value, centerX, centerY);

        // Etiqueta
        ctx.font = "14px Sora";
        ctx.fillStyle = "#6b7280";

        ctx.fillText(options.label, centerX, centerY + 40);

        ctx.restore();

    }

};
Chart.register(centerTextPlugin);

document.addEventListener('DOMContentLoaded', () =>{
    const canvas = document.getElementById("difficultyChart");

    if (!canvas) {
        return;
    }

    const easy = Number(canvas.dataset.easy);
    const medium = Number(canvas.dataset.medium);
    const hard = Number(canvas.dataset.hard);
    const total = Number(canvas.dataset.total);


    new Chart(canvas, {
        type: "doughnut",
        data: {
            labels: ['Fácil', 'Intermedio', 'Difícil'],
            datasets: [{
                label: "Nº de cuestionarios",
                data: [easy, medium, hard],
                backgroundColor: ["#52AD36", "#FAB337", "#EB4D4D"],
                borderWidht: 0
            }]
        },

        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins:{
                legend:{
                    position: "top"
                },
                centerText: {
                    value: total,
                    label: "Cuestionarios"
                }
            },
            cutout: "70%"
        }
    });
});