package com.quizly.app.model.view.tarjeta;

import com.quizly.app.model.entity.Mazo;

public record MazoResumen(Mazo mazo, long total, long pendientes) {
}
