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
    private int maxReachedQuestionIndex;
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

    public void recordResponse(UserQuizResponse response){
        int existingIndex = -1;
        for (int i = 0; i < responses.size(); i++) {
            if (responses.get(i).preguntaId().equals(response.preguntaId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            responses.set(existingIndex, response);
        } else {
            responses.add(response);
        }
        recalculateScore();
    }

    public void recalculateScore(){
        this.score = (int) responses.stream().filter(UserQuizResponse::correcta).count();
    }

    public void addResponse(UserQuizResponse response){
        recordResponse(response);
    }

    public Long getSelectedOptionIdForQuestion(Long preguntaId){
        if (preguntaId == null) {
            return null;
        }
        return responses.stream()
            .filter(r -> r.preguntaId().equals(preguntaId))
            .map(UserQuizResponse::opcionSeleccionadaId)
            .findFirst()
            .orElse(null);
    }

    public boolean isQuestionAnswered(Long preguntaId){
        if (preguntaId == null) {
            return false;
        }
        return responses.stream().anyMatch(r -> r.preguntaId().equals(preguntaId));
    }

    public boolean canNavigateTo(int targetIndex){
        return targetIndex >= 0 && targetIndex <= maxReachedQuestionIndex && targetIndex < preguntas.size();
    }

    public QuestionQuizRecord getCurrentQuestion(){
        return preguntas.get(currentQuestionIndex);
    }

    public boolean isFinished(){
        return currentQuestionIndex >= preguntas.size();
    }

    public int getProgress(){
        if (preguntas == null || preguntas.isEmpty()) {
            return 0;
        }
        return (int) ((responses.size() * 100.0) / preguntas.size());
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
