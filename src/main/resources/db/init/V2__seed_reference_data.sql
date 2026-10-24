-- Quizly - Datos de referencia iniciales

-- Tipos de pregunta
INSERT INTO tipo_preguntas (codigo, nombre) VALUES
    ('SINGLE_CHOICE', 'Opción múltiple'),
    ('TRUE_FALSE',    'Verdadero o falso');

-- Estados de intento de cuestionario
INSERT INTO intentos_cuestionario_estados (codigo, nombre) VALUES
    ('IN_PROGRESS', 'En progreso'),
    ('FINISHED',    'Finalizado'),
    ('ABANDONNED',  'Abandonado');

-- Generos
INSERT INTO generos (codigo, nombre) VALUES
    ('M', 'Hombre'),
    ('F', 'Mujer'),
    ('PND', 'Prefiero no decirlo');

-- Dificultades
INSERT INTO dificultades (codigo, nombre) VALUES('EASY', 'Fácil');
INSERT INTO dificultades (codigo, nombre) VALUES('MEDIUM', 'Intermedio');
INSERT INTO dificultades (codigo, nombre) VALUES('HARD', 'Difícil');
