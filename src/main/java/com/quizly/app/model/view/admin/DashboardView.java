package com.quizly.app.model.view.admin;

import java.util.List;

import com.quizly.app.model.view.components.StatCardView;
import com.quizly.app.model.view.quiz.QuizListItemView;

import lombok.Builder;

@Builder
public record DashboardView(
   List<StatCardView> stats,
   List<QuizListItemView> cuestionariosRecientes) {}
