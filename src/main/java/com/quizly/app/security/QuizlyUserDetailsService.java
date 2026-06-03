package com.quizly.app.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizly.app.dao.UsuarioDao;
import com.quizly.app.model.entity.Usuario;

@Service
@Transactional(readOnly = true)
public class QuizlyUserDetailsService implements UserDetailsService {

    private final UsuarioDao usuarioDao;

    public QuizlyUserDetailsService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con el nombre indicado"));

        return new QuizlyUserDetails(usuario);
    }
}
