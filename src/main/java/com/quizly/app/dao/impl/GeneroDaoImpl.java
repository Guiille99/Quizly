package com.quizly.app.dao.impl;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.GeneroDao;
import com.quizly.app.model.entity.Genero;

@Repository
public class GeneroDaoImpl extends AbstractDaoImpl<Genero, Long> implements GeneroDao{

    protected GeneroDaoImpl() {
        super(Genero.class);
    }

}
