package com.quizly.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.enums.TrendType;
import com.quizly.app.model.view.admin.DashboardView;
import com.quizly.app.model.view.common.TrendIndicatorView;
import com.quizly.app.model.view.components.QuizDifficultyChartView;
import com.quizly.app.model.view.components.StatCardView;
import com.quizly.app.model.view.quiz.QuizListItemView;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.PreguntaRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.util.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UsuarioRepository usuarioRepository;
    private final CuestionarioRepository cuestionarioRepository;
    private final PreguntaRepository preguntaRepository;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final Utils utils;

    public DashboardView getDashboard(){
        return DashboardView.builder()
            .stats(getStats())
            .cuestionariosRecientes(getRecentQuizzes())
            .difficultyChart(geDifficultyChart())
            .build();
    }

    private List<StatCardView> getStats(){
        return List.of(
            buildUserStat(),
            buildQuizStat(),
            buildQuestionStat(),
            buildAttemptStat()
        );
    }

    private StatCardView buildUserStat() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentPeriodStart = now.minusDays(30);
        LocalDateTime previousPeriodStart = currentPeriodStart.minusDays(30);

        long totalUsers = usuarioRepository.count();

        long currentPeriodUsers = usuarioRepository.countByCreatedAtBetween(currentPeriodStart, now);
        long previousPeriodUsers = usuarioRepository.countByCreatedAtBetween(previousPeriodStart, currentPeriodStart);
        
        return StatCardView.builder()
                .titulo("Usuarios")
                .icono("fa-regular fa-user")
                .color("blue")
                .valor(String.valueOf(totalUsers))
                .tendencia(calculateTrend(currentPeriodUsers, previousPeriodUsers))
                .build();
    }

    private StatCardView buildQuizStat(){
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentPeriodStart = now.minusDays(30);
        LocalDateTime previousPeriodStart = currentPeriodStart.minusDays(30);

        long totalQuizzies = cuestionarioRepository.count();

        long currentPeriodQuizzies = cuestionarioRepository.countByCreatedAtBetween(currentPeriodStart, now);
        long previousPeriodQuizzies = cuestionarioRepository.countByCreatedAtBetween(previousPeriodStart, currentPeriodStart);

        return StatCardView.builder()
                .titulo("Cuestionarios")
                .icono("fa-solid fa-book")
                .color("green")
                .valor(String.valueOf(totalQuizzies))
                .tendencia(calculateTrend(currentPeriodQuizzies, previousPeriodQuizzies))
                .build();
    }

    private StatCardView buildQuestionStat(){
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentPeriodStart = now.minusDays(30);
        LocalDateTime previousPeriodStart = currentPeriodStart.minusDays(30);

        long totalQuestions = preguntaRepository.count();

        long currentPeriodQuestions = preguntaRepository.countByCreatedAtBetween(currentPeriodStart, now);
        long previousPeriodQuestions = preguntaRepository.countByCreatedAtBetween(previousPeriodStart, currentPeriodStart);

        return StatCardView.builder()
                .titulo("Preguntas")
                .icono("fa-regular fa-circle-question")
                .color("orange")
                .valor(String.valueOf(totalQuestions))
                .tendencia(calculateTrend(currentPeriodQuestions, previousPeriodQuestions))
                .build();
    }

    private StatCardView buildAttemptStat(){
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentPeriodStart = now.minusDays(30);
        LocalDateTime previousPeriodStart = currentPeriodStart.minusDays(30);

        long totalAttempts = intentoCuestionarioRepository.count();

        long currentPeriodAttempts = intentoCuestionarioRepository.countByCreatedAtBetween(currentPeriodStart, now);
        long previousPeriodAttempts = intentoCuestionarioRepository.countByCreatedAtBetween(previousPeriodStart, currentPeriodStart);

        return StatCardView.builder()
                .titulo("Intentos")
                .icono("fa-regular fa-circle-question")
                .color("purple")
                .valor(String.valueOf(totalAttempts))
                .tendencia(calculateTrend(currentPeriodAttempts, previousPeriodAttempts))
                .build();
    }

    private List<QuizListItemView> getRecentQuizzes(){
        List<Cuestionario> cuestionariosRecientes = cuestionarioRepository.findLastModifiedLimit(5);
        return cuestionariosRecientes.stream()
            .map(this::toQuizListItem)
            .toList();
    }

    private QuizListItemView toQuizListItem(Cuestionario cuestionario) {
        return QuizListItemView.builder()
            .id(cuestionario.getId())
            .slug(cuestionario.getSlug())
            .titulo(cuestionario.getTitulo())
            .tema(cuestionario.getTema().getNombre())
            .dificultad(cuestionario.getDificultad())
            .numeroPreguntas(cuestionario.getPreguntas().size())
            .version(cuestionario.getVersion())
            .fechaActualizacion(utils.formatFechaRelativa(cuestionario.getUpdatedAt()))
            .build();
    }

    /**
     * Calcula la tendencia entre dos periodos consecutivos a partir del porcentaje
     * de variación entre ambos.
     * 
     * <p>
     * El resultado contiene tanto el tipo de tendencia (positiva, negativa o neutra)
     * como la etiqueta que se mostrará en la interfaz ("+25%", "-10%", "Nuevo")
     * </p>
     * 
     * <p>
     * Si el periodo anterior no contiene registros:
     * </p>
     * 
     * <ul>
     *  <li>Si ambos periodos son cero, la tendencia se considera neutra (0%).</li>
     *  <li>Si el periodo actual es mayor que cero, la tendencia se considera positiva y muestra la etiqueta "Nuevo".</li>
     * </ul>
     * @param currentPeriod     número de registros del periodo actual.
     * @param previousPeriod    número de registros del periodo inmediatamente anterior.  
     * @return  indicador de tendencia que será mostrado en la interfaz.
     */
    private TrendIndicatorView calculateTrend(long currentPeriod, long previousPeriod){
        if (previousPeriod == 0) {
            if (currentPeriod == 0) {
                return new TrendIndicatorView(TrendType.NEUTRAL, "0%");
            }

            return new TrendIndicatorView(TrendType.POSITIVE, "Nuevo");
        }

        double variation = ((double) (currentPeriod - previousPeriod) / previousPeriod) * 100;

        TrendType type;

        if (variation > 0) {
            type = TrendType.POSITIVE;
        } else if(variation < 0){
            type = TrendType.NEGATIVE;
        } else{
            type = TrendType.NEUTRAL;
        }

        return new TrendIndicatorView(type, String.format("%+.0f%%", variation));
    }

    private QuizDifficultyChartView geDifficultyChart(){
        return QuizDifficultyChartView.builder()
            .easy(cuestionarioRepository.countByDificultadCodigo("EASY"))
            .medium(cuestionarioRepository.countByDificultadCodigo("MEDIUM"))
            .hard(cuestionarioRepository.countByDificultadCodigo("HARD"))
            .build();
    }
}
