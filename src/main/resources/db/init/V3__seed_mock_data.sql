-- Quizly - Generación de datos de prueba (Mock Data)

DO $$
DECLARE
    i INTEGER;
    gen_id_m BIGINT;
    gen_id_f BIGINT;
    gen_id_pnd BIGINT;
    diff_easy_id BIGINT;
    diff_medium_id BIGINT;
    diff_hard_id BIGINT;
    tipo_single_id BIGINT;
    estado_finished_id BIGINT;
    
    -- Arrays para datos realistas
    nombres_m TEXT[] := ARRAY['Juan', 'Carlos', 'Miguel', 'Antonio', 'Pedro', 'Javier', 'Diego', 'Raúl', 'Fernando', 'José', 'Manuel', 'Francisco', 'David', 'Daniel', 'Pablo', 'Sergio', 'Alejandro', 'Adrián', 'Álvaro', 'Jorge', 'Rubén', 'Alberto', 'Iván', 'Mario', 'Óscar', 'Ricardo', 'Roberto', 'Luis', 'Ramón', 'Andrés', 'Vicente', 'Joaquín', 'Ignacio', 'Emilio', 'Rafael', 'Gonzalo', 'Esteban', 'Marcos', 'Cristian', 'Héctor', 'Guillermo', 'Arturo', 'Felipe', 'Gabriel', 'Israel', 'Julián', 'Lorenzo', 'Martín', 'Nicolás', 'Óliver'];
    nombres_f TEXT[] := ARRAY['María', 'Lucía', 'Ana', 'Elena', 'Beatriz', 'Isabel', 'Carmen', 'Sofía', 'Laura', 'Marta', 'Paula', 'Andrea', 'Sara', 'Nuria', 'Cristina', 'Patricia', 'Rocío', 'Silvia', 'Teresa', 'Verónica', 'Julia', 'Marina', 'Alba', 'Irene', 'Eva', 'Clara', 'Diana', 'Gloria', 'Inés', 'Lorena', 'Mónica', 'Natalia', 'Pilar', 'Rosa', 'Sandra', 'Susana', 'Victoria', 'Yolanda', 'Ángela', 'Aurora', 'Blanca', 'Carolina', 'Daniela', 'Esther', 'Fátima', 'Gabriela', 'Helena', 'Jimena', 'Lidia', 'Manuela'];
    apellidos TEXT[] := ARRAY['García', 'Rodríguez', 'López', 'Sánchez', 'Martínez', 'Pérez', 'Gómez', 'Martin', 'Jiménez', 'Ruiz', 'Hernández', 'Díaz', 'Moreno', 'Muñoz', 'Álvarez', 'Romero', 'Alonso', 'Navarro', 'Torres', 'Domínguez', 'Vázquez', 'Ramos', 'Gil', 'Ramírez', 'Serrano', 'Blanco', 'Molina', 'Suárez', 'Ortega', 'Delgado', 'Castro', 'Ortiz', 'Rubio', 'Marín', 'Sanz', 'Iglesias', 'Nuñez', 'Medina', 'Garrido', 'Cortés', 'Castillo', 'Santos', 'Lozano', 'Guerrero', 'Cano', 'Prieto', 'Méndez', 'Cruz', 'Calvo', 'Herrera'];
    asignaturas_nombres TEXT[] := ARRAY['Matemáticas', 'Historia', 'Física', 'Química', 'Literatura', 'Geografía', 'Biología', 'Arte', 'Filosofía', 'Inglés'];
BEGIN
    -- 1. Asegurar Datos de Referencia (Dificultades que no estaban en V2)
    INSERT INTO dificultades (codigo, nombre) VALUES 
        ('EASY', 'Fácil'), ('MEDIUM', 'Intermedio'), ('HARD', 'Difícil')
    ON CONFLICT (codigo) DO NOTHING;

    -- Obtener IDs de referencia para las FKs
    SELECT id INTO gen_id_m FROM generos WHERE codigo = 'M';
    SELECT id INTO gen_id_f FROM generos WHERE codigo = 'F';
    SELECT id INTO gen_id_pnd FROM generos WHERE codigo = 'PND';
    SELECT id INTO diff_easy_id FROM dificultades WHERE codigo = 'EASY';
    SELECT id INTO diff_medium_id FROM dificultades WHERE codigo = 'MEDIUM';
    SELECT id INTO diff_hard_id FROM dificultades WHERE codigo = 'HARD';
    SELECT id INTO tipo_single_id FROM tipo_preguntas WHERE codigo = 'SINGLE_CHOICE';
    SELECT id INTO estado_finished_id FROM intentos_cuestionario_estados WHERE codigo = 'FINISHED';

    -- 2. Generar 50 ASIGNATURAS
    FOR i IN 1..50 LOOP
        INSERT INTO asignaturas (nombre, descripcion, slug, created_at, updated_at)
        VALUES (
            asignaturas_nombres[(i % 10) + 1] || ' Nivel ' || i,
            'Descripción detallada para la asignatura de ' || asignaturas_nombres[(i % 10) + 1] || ' grado ' || i,
            'asig-' || i || '-' || lower(replace(asignaturas_nombres[(i % 10) + 1], ' ', '-')),
            NOW() - (i || ' days')::interval,
            NOW()
        );
    END LOOP;

    -- 3. Generar 50 USUARIOS (Mezcla de Admin y Estudiantes)
    FOR i IN 1..50 LOOP
        DECLARE
            es_mujer BOOLEAN := (i % 2 = 0); -- Alterna entre hombre y mujer
            nombre_elegido TEXT := CASE WHEN es_mujer THEN nombres_f[(i - 1) % array_length(nombres_f, 1) + 1] ELSE nombres_m[(i - 1) % array_length(nombres_m, 1) + 1] END;
            apellido_elegido TEXT := apellidos[(i - 1) % array_length(apellidos, 1) + 1] || ' ' || apellidos[(i % array_length(apellidos, 1)) + 1]; -- Combina dos apellidos
            u_role TEXT := CASE WHEN i = 1 THEN 'ADMIN' ELSE 'ESTUDIANTE' END;
            u_gen BIGINT := CASE WHEN es_mujer THEN gen_id_f ELSE gen_id_m END;
        BEGIN
            INSERT INTO usuarios (nombre, apellidos, username, email, password, rol, genero_id, created_at, updated_at)
            VALUES (
                nombre_elegido,
                apellido_elegido,
                'user' || i,
                'usuario' || i || '@quizly.app',
                '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.z.icLfy', -- password123
                u_role,
                u_gen,
                NOW() - (i || ' hours')::interval,
                NOW()
            );
        END;
    END LOOP;

    -- 4. Generar 50 TEMAS (Repartidos entre las primeras 10 asignaturas)
    FOR i IN 1..50 LOOP
        INSERT INTO temas (nombre, descripcion, slug, orden, created_at, updated_at, asignatura_id)
        VALUES (
            'Tema ' || i,
            'Contenido detallado del tema número ' || i,
            'tema-' || i,
            i,
            NOW(),
            NOW(),
            (SELECT id FROM asignaturas ORDER BY id LIMIT 1 OFFSET (i % 10))
        );
    END LOOP;

    -- 5. Generar 50 CUESTIONARIOS
    FOR i IN 1..50 LOOP
        INSERT INTO cuestionarios (titulo, descripcion, tiempo_limite, slug, version, created_at, updated_at, tema_id, dificultad_id)
        VALUES (
            'Quiz de evaluación ' || i,
            'Pon a prueba tus conocimientos del tema ' || i,
            20,
            'quiz-evaluacion-' || i,
            1,
            NOW(),
            NOW(),
            (SELECT id FROM temas ORDER BY id LIMIT 1 OFFSET (i - 1)),
            CASE WHEN i % 3 = 0 THEN diff_hard_id WHEN i % 2 = 0 THEN diff_medium_id ELSE diff_easy_id END
        );
    END LOOP;

    -- 6. Generar 50 PREGUNTAS (Una por cuestionario para simplificar el mock)
    FOR i IN 1..50 LOOP
        INSERT INTO preguntas (enunciado, tipo_pregunta_id, cuestionario_id)
        VALUES (
            '¿Cuál es el resultado de la operación fundamental en el contexto del tema ' || i || '?',
            tipo_single_id,
            (SELECT id FROM cuestionarios ORDER BY id LIMIT 1 OFFSET (i - 1))
        );
    END LOOP;

    -- 7. Generar OPCIONES DE RESPUESTA (4 por cada pregunta = 200 registros)
    FOR i IN 1..50 LOOP
        DECLARE
            preg_id BIGINT;
        BEGIN
            SELECT id INTO preg_id FROM preguntas ORDER BY id LIMIT 1 OFFSET (i - 1);
            
            INSERT INTO opciones_respuesta (texto, correcta, retroalimentacion, pregunta_id) VALUES
                ('Opción A (Correcta)', TRUE, '¡Excelente trabajo!', preg_id),
                ('Opción B (Incorrecta)', FALSE, 'Cerca, pero no es la respuesta.', preg_id),
                ('Opción C (Incorrecta)', FALSE, 'Revisa el material del tema.', preg_id),
                ('Opción D (Incorrecta)', FALSE, 'Esta opción es totalmente errónea.', preg_id);
        END;
    END LOOP;

    -- 8. Generar 50 INTENTOS DE CUESTIONARIO
    FOR i IN 1..50 LOOP
        INSERT INTO intentos_cuestionario (cuestionario_id, usuario_id, fecha_inicio, fecha_fin, aciertos, fallos, tiempo_empleado, estado_id)
        VALUES (
            (SELECT id FROM cuestionarios ORDER BY id LIMIT 1 OFFSET (i % 10)),
            (SELECT id FROM usuarios WHERE rol = 'ESTUDIANTE' ORDER BY id LIMIT 1 OFFSET (i - 1)),
            NOW() - INTERVAL '1 hour',
            NOW(),
            8,
            2,
            600000, -- 10 minutos
            estado_finished_id
        );
    END LOOP;

    -- 9. Generar 50 RESPUESTAS DE USUARIO
    FOR i IN 1..50 LOOP
        DECLARE
            v_intento_id BIGINT;
            v_pregunta_id BIGINT;
            v_opcion_id BIGINT;
        BEGIN
            SELECT id INTO v_intento_id FROM intentos_cuestionario ORDER BY id LIMIT 1 OFFSET (i - 1);
            -- Buscamos la pregunta asociada al cuestionario del intento
            SELECT p.id INTO v_pregunta_id FROM preguntas p 
                JOIN intentos_cuestionario ic ON ic.cuestionario_id = p.cuestionario_id 
                WHERE ic.id = v_intento_id LIMIT 1;
            -- Seleccionamos la opción correcta
            SELECT id INTO v_opcion_id FROM opciones_respuesta WHERE pregunta_id = v_pregunta_id AND correcta = TRUE LIMIT 1;

            IF v_pregunta_id IS NOT NULL AND v_opcion_id IS NOT NULL THEN
                INSERT INTO respuesta_usuario (correcta, intento_id, pregunta_id, opcion_id)
                VALUES (TRUE, v_intento_id, v_pregunta_id, v_opcion_id);
            END IF;
        END;
    END LOOP;

    -- 10. Generar 50 PASSWORD RESET TOKENS
    FOR i IN 1..50 LOOP
        INSERT INTO password_reset_tokens (token, usuario_id, fecha_expiracion)
        VALUES (gen_random_uuid()::text, (SELECT id FROM usuarios ORDER BY id LIMIT 1 OFFSET (i - 1)), NOW() + INTERVAL '24 hours');
    END LOOP;
END $$;
