package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.quizly.app.dto.form.QuizForm;

import tools.jackson.databind.json.JsonMapper;

class QuizJsonImportServiceTest {
    private QuizJsonImportService service;

    @BeforeEach
    void setUp() {
        service = new QuizJsonImportService(JsonMapper.builder().build());
    }

    @Test
    void importaContenidoSinAlterarMetadatosDelFormulario() {
        QuizForm form = new QuizForm();
        form.setAsignaturaId(12L);
        form.setTemaId(34L);
        form.setDificultadId(56L);
        form.setTitulo("Título anterior");

        QuizForm imported = service.importIntoForm(file("""
                {
                  "titulo": "Biología",
                  "descripcion": "Células",
                  "preguntas": [{
                    "enunciado": "¿Qué estructura contiene el ADN?",
                    "tipoPreguntaCodigo": "SINGLE_CHOICE",
                    "opcionesRespuesta": [
                      {"texto": "Núcleo", "correcta": true},
                      {"texto": "Membrana", "correcta": false, "retroalimentacion": ""}
                    ]
                  }]
                }
                """), form);

        assertEquals("Biología", imported.getTitulo());
        assertEquals("Células", imported.getDescripcion());
        assertEquals(1, imported.getPreguntas().size());
        assertEquals("SINGLE_CHOICE", imported.getPreguntas().get(0).getTipoPreguntaCodigo());
        assertTrue(imported.getPreguntas().get(0).getOpcionesRespuesta().get(0).isCorrecta());
        assertEquals(12L, imported.getAsignaturaId());
        assertEquals(34L, imported.getTemaId());
        assertEquals(56L, imported.getDificultadId());
    }

    @Test
    void rechazaJsonConTipoNoAdmitidoSinCambiarFormulario() {
        QuizForm form = new QuizForm();
        form.setTitulo("Título existente");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.importIntoForm(file("""
                        {"titulo":"Nuevo","preguntas":[{
                          "enunciado":"Pregunta","tipoPreguntaCodigo":"MULTIPLE",
                          "opcionesRespuesta":[]
                        }]}
                        """), form));

        assertTrue(exception.getMessage().contains("no está admitido"));
        assertEquals("Título existente", form.getTitulo());
        assertTrue(form.isEditable());
    }

    @Test
    void rechazaVariasRespuestasCorrectas() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.importIntoForm(file("""
                        {"titulo":"Nuevo","preguntas":[{
                          "enunciado":"Pregunta","tipoPreguntaCodigo":"SINGLE_CHOICE",
                          "opcionesRespuesta":[
                            {"texto":"A","correcta":true},
                            {"texto":"B","correcta":true}
                          ]
                        }]}
                        """), new QuizForm()));

        assertTrue(exception.getMessage().contains("exactamente una opción correcta"));
    }

    @Test
    void rechazaClavesDesconocidas() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.importIntoForm(file("""
                        {"titulo":"Nuevo","preguntas":[],"asignaturaId":5}
                        """), new QuizForm()));

        assertTrue(exception.getMessage().contains("Campo no reconocido"));
    }

    @Test
    void importaVerdaderoFalsoConOpcionesFijasEnOrden() {
        QuizForm imported = service.importIntoForm(file("""
                {"titulo":"Nuevo","preguntas":[{
                  "enunciado":"La Tierra es redonda.","tipoPreguntaCodigo":"TRUE_FALSE",
                  "opcionesRespuesta":[
                    {"texto":"Verdadero","correcta":true},
                    {"texto":"Falso","correcta":false}
                  ]
                }]}
                """), new QuizForm());

        assertEquals("TRUE_FALSE", imported.getPreguntas().get(0).getTipoPreguntaCodigo());
        assertEquals("Verdadero", imported.getPreguntas().get(0).getOpcionesRespuesta().get(0).getTexto());
    }

    @Test
    void rechazaJsonMalFormado() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.importIntoForm(file("{\"titulo\":"), new QuizForm()));

        assertTrue(exception.getMessage().contains("JSON válido"));
    }

      @Test
      void rechazaArchivoQueNoTieneExtensionJsonAunqueSuContenidoSeaJson() {
        MockMultipartFile file = new MockMultipartFile(
            "jsonFile", "quiz.txt", "application/json", "{\"titulo\":\"Nuevo\"}".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.importIntoForm(file, new QuizForm()));

        assertTrue(exception.getMessage().contains("extensión .json"));
      }

    @Test
    void rechazaOrdenDeOpcionesVerdaderoFalsoNoEditable() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.importIntoForm(file("""
                        {"titulo":"Nuevo","preguntas":[{
                          "enunciado":"La Tierra es redonda.","tipoPreguntaCodigo":"TRUE_FALSE",
                          "opcionesRespuesta":[
                            {"texto":"Sí","correcta":true},
                            {"texto":"No","correcta":false}
                          ]
                        }]}
                        """), new QuizForm()));

        assertTrue(exception.getMessage().contains("Verdadero"));
    }

    private MockMultipartFile file(String json) {
        return new MockMultipartFile("jsonFile", "quiz.json", "application/json", json.getBytes());
    }
}