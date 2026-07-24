package com.quizly.app.model.session;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserQuizSession {
    public static final String SESSION_KEY = "QUIZ_SESSION";

    private Long intentoCuestionarioId;
    private Long cuestionarioId;
    private String titulo;
    private String tema;
    private String dificultad;
    private String slug;
    private List<QuestionQuizRecord> preguntas = new ArrayList<>();
    private int currentQuestionIndex;
    private int score;
    private Instant startedAt;
    private List<UserQuizResponse> responses = new ArrayList<>();
    private LocalDateTime fechaExpiracion;
    private LocalDateTime fechaUltimaActividad;

    public void nextQuestion(){
        currentQuestionIndex++;
    }

    public void increaseScore(){
        score++;
    }

    public void addResponse(UserQuizResponse response){
        responses.add(response);
    }

    public QuestionQuizRecord getCurrentQuestion(){
        return preguntas.get(currentQuestionIndex);
    }

    public boolean isFinished(){
        return currentQuestionIndex >= preguntas.size();
    }

    public int getProgress(){
        return (int) ((currentQuestionIndex * 100.0) / preguntas.size());
    }

    public boolean hasTiempoLimite(){
        return fechaExpiracion != null;
    }

    public boolean isTiempoAgotado(){
        return hasTiempoLimite() && !LocalDateTime.now().isBefore(fechaExpiracion);
    }

    public long getSegundosRestantes(){
        if (!hasTiempoLimite()) {
            return -1;
        }

        Duration duration = Duration.between(LocalDateTime.now(), fechaExpiracion);

        long millis = duration.toMillis();

        long segundos = (long) Math.ceil(millis / 1000.0);
        
        return Math.max(segundos, 0);
    }
}
