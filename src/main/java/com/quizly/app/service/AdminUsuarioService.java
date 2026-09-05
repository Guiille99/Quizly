package com.quizly.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.form.UsuarioFilterForm;
import com.quizly.app.dto.form.UsuarioForm;
import com.quizly.app.mapper.UsuarioViewMapper;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.view.usuario.UsuarioView;
import com.quizly.app.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AdminUsuarioService {
    private final UsuarioRepository usuarioRepository;
    
    private final UsuarioViewMapper usuarioViewMapper;

    public Page<UsuarioView> getUsuarios(UsuarioFilterForm filters, Pageable pageable) {
        Page<Usuario> page = usuarioRepository.findAllWithFilters(filters, pageable);
        return page.map(usuarioViewMapper::toUsuarioView);
    }

    public UsuarioForm createUsuarioForm() {
        return new UsuarioForm();
    }

    public UsuarioForm createUsuarioForm(Usuario usuario) {
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.getId());
        form.setNombre(usuario.getNombre());
        form.setApellidos(usuario.getApellidos());
        form.setUsername(usuario.getUsername());
        form.setEmail(usuario.getEmail());
        form.setRol(usuario.getRol());
        if (usuario.getGenero() != null) {
            form.setGeneroId(usuario.getGenero().getId());
        }
        return form;
    }
}
