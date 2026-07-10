package com.quizly.app.model.view.quiz;

import java.util.List;

public record QuizSummary(

    String titulo,
    String tema,
    String dificultad,
    String slug,
    int totalQuestions,
    int correctAnswers,
    int incorrectAnswers,
    int scorePercentage,
    String tiempoEmpleado,
    List<QuestionSummary> questions
) {

    public String getResultMessage() {

        if (scorePercentage >= 90) {
            return "Has demostrado un excelente dominio del tema.";
        }

        if (scorePercentage >= 75) {
            return "Tienes un muy buen dominio del contenido, aunque aún hay margen de mejora.";
        }

        if (scorePercentage >= 60) {
            return "Has superado el cuestionario con un buen resultado. Sigue practicando para consolidar conocimientos.";
        }

        if (scorePercentage >= 40) {
            return "Has acertado una parte importante del cuestionario, pero conviene repasar algunos conceptos.";
        }

        return "Te recomendamos revisar el contenido y volver a intentarlo. ¡Seguro que mejorarás!";
    }
}
