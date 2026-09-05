package com.quizly.app.model.view.usuario;

import lombok.Builder;

@Builder 
public record UsuarioView(
    Long id,
    String nombreCompleto,
    String username,
    String email,
    String rol,
    String iniciales,
    int nIntentos
) {}
