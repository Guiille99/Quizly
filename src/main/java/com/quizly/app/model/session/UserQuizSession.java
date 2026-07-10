package com.quizly.app.model.session;

import java.time.Instant;
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
}
