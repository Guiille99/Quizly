package com.quizly.app.mapper;

import org.springframework.stereotype.Component;

import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.view.usuario.UsuarioView;

@Component 
public class UsuarioViewMapper {

    public UsuarioView toUsuarioView(Usuario usuario){
        return UsuarioView.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombre() + " " + usuario.getApellidos())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .rol(usuario.getRol() != null ? usuario.getRol().name() : "Sin Rol")
                .iniciales(usuario.getIniciales())
                .nIntentos(usuario.getIntentos() != null ? usuario.getIntentos().size() : 0)
                .build();
    }
}
