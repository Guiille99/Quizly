package com.quizly.app.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserQuizSessionTest {

    private UserQuizSession session;

    @BeforeEach
    void setUp() {
        session = new UserQuizSession();
        List<QuestionQuizRecord> questions = List.of(
            new QuestionQuizRecord(1L, "Pregunta 1", List.of(
                new OptionQuizRecord(101L, "Opción 1A", true, null),
                new OptionQuizRecord(102L, "Opción 1B", false, null)
            )),
            new QuestionQuizRecord(2L, "Pregunta 2", List.of(
                new OptionQuizRecord(201L, "Opción 2A", false, null),
                new OptionQuizRecord(202L, "Opción 2B", true, null)
            )),
            new QuestionQuizRecord(3L, "Pregunta 3", List.of(
                new OptionQuizRecord(301L, "Opción 3A", true, null),
                new OptionQuizRecord(302L, "Opción 3B", false, null)
            ))
        );
        session.setPreguntas(questions);
        session.setCurrentQuestionIndex(0);
        session.setMaxReachedQuestionIndex(0);
    }

    @Test
    @DisplayName("canNavigateTo solo permite índices entre 0 y maxReachedQuestionIndex")
    void canNavigateToAllowsOnlyUpToMaxReached() {
        session.setMaxReachedQuestionIndex(1);

        assertTrue(session.canNavigateTo(0), "Debe poder navegar al índice 0");
        assertTrue(session.canNavigateTo(1), "Debe poder navegar al índice 1");
        assertFalse(session.canNavigateTo(2), "No debe poder navegar al índice 2");
        assertFalse(session.canNavigateTo(-1), "No debe poder navegar a índices negativos");
        assertFalse(session.canNavigateTo(10), "No debe poder navegar fuera de rango");
    }

    @Test
    @DisplayName("recordResponse añade respuesta nueva y recalcula el score")
    void recordResponseAddsNewResponseAndRecalculatesScore() {
        session.recordResponse(new UserQuizResponse(1L, 101L, "Opción 1A", true));

        assertEquals(1, session.getResponses().size());
        assertEquals(1, session.getScore());
        assertEquals(101L, session.getSelectedOptionIdForQuestion(1L));
        assertTrue(session.isQuestionAnswered(1L));
        assertNull(session.getSelectedOptionIdForQuestion(2L));
        assertFalse(session.isQuestionAnswered(2L));
    }

    @Test
    @DisplayName("recordResponse actualiza respuesta existente sin duplicar y recalcula score correctamente")
    void recordResponseUpdatesExistingResponseWithoutDuplicates() {
        // Primera respuesta correcta
        session.recordResponse(new UserQuizResponse(1L, 101L, "Opción 1A", true));
        assertEquals(1, session.getResponses().size());
        assertEquals(1, session.getScore());

        // El usuario vuelve a la pregunta 1 y la cambia por una incorrecta
        session.recordResponse(new UserQuizResponse(1L, 102L, "Opción 1B", false));
        assertEquals(1, session.getResponses().size(), "No debe duplicar la respuesta en la lista");
        assertEquals(0, session.getScore(), "El score debe decrementar al corregir por una opción errónea");
        assertEquals(102L, session.getSelectedOptionIdForQuestion(1L));

        // El usuario vuelve a cambiarla por la opción correcta
        session.recordResponse(new UserQuizResponse(1L, 101L, "Opción 1A", true));
        assertEquals(1, session.getResponses().size());
        assertEquals(1, session.getScore());
    }

    @Test
    @DisplayName("getProgress calcula el avance en base a las respuestas completadas")
    void getProgressCalculatesBasedOnResponses() {
        assertEquals(0, session.getProgress());

        session.recordResponse(new UserQuizResponse(1L, 101L, "Opción 1A", true));
        // 1 de 3 respuestas -> 33%
        assertEquals(33, session.getProgress());

        session.recordResponse(new UserQuizResponse(2L, 202L, "Opción 2B", true));
        // 2 de 3 respuestas -> 66%
        assertEquals(66, session.getProgress());
    }
}

