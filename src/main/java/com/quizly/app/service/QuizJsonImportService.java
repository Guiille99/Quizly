package com.quizly.app.service;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.quizly.app.dto.form.OpcionRespuestaForm;
import com.quizly.app.dto.form.PreguntaForm;
import com.quizly.app.dto.form.QuizForm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class QuizJsonImportService {
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> QUIZ_FIELDS = Set.of("titulo", "descripcion", "preguntas");
    private static final Set<String> QUESTION_FIELDS = Set.of("enunciado", "tipoPreguntaCodigo", "opcionesRespuesta");
    private static final Set<String> ANSWER_FIELDS = Set.of("texto", "correcta", "retroalimentacion");

    private final ObjectMapper objectMapper;

    public QuizJsonImportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public QuizForm importIntoForm(MultipartFile file, QuizForm form) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo JSON para importar.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new IllegalArgumentException("El formato no está permitido. Adjunta un archivo con extensión .json.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo JSON no puede superar los 2 MB.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(file.getBytes());
        } catch (Exception exception) {
            throw new IllegalArgumentException("El archivo no contiene un JSON válido.", exception);
        }

        requireObject(root, "El contenido debe ser un objeto JSON.");
        rejectUnknownFields(root, QUIZ_FIELDS, "cuestionario");

        String titulo = requiredText(root, "titulo", "El título es obligatorio.", 100);
        String descripcion = optionalText(root, "descripcion", "La descripción", 255);
        JsonNode questionsNode = root.get("preguntas");
        if (questionsNode == null || !questionsNode.isArray() || questionsNode.isEmpty()) {
            throw new IllegalArgumentException("El campo 'preguntas' debe contener al menos una pregunta.");
        }

        ArrayList<PreguntaForm> questions = new ArrayList<>();
        for (int index = 0; index < questionsNode.size(); index++) {
            questions.add(parseQuestion(questionsNode.get(index), index + 1));
        }

        QuizForm result = form == null ? new QuizForm() : form;
        result.setTitulo(titulo);
        result.setDescripcion(descripcion);
        result.setPreguntas(questions);
        result.setEditable(true);
        return result;
    }

    private PreguntaForm parseQuestion(JsonNode node, int questionNumber) {
        String location = "La pregunta " + questionNumber;
        requireObject(node, location + " debe ser un objeto.");
        rejectUnknownFields(node, QUESTION_FIELDS, location);

        String enunciado = requiredText(node, "enunciado", location + ": el enunciado es obligatorio.", 255);
        String typeCode = requiredText(node, "tipoPreguntaCodigo", location + ": el tipo es obligatorio.", 30);
        JsonNode answersNode = node.get("opcionesRespuesta");
        if (answersNode == null || !answersNode.isArray()) {
            throw new IllegalArgumentException(location + ": 'opcionesRespuesta' debe ser una lista.");
        }

        int minAnswers;
        int maxAnswers;
        switch (typeCode) {
            case "SINGLE_CHOICE" -> {
                minAnswers = 2;
                maxAnswers = 4;
            }
            case "TRUE_FALSE" -> {
                minAnswers = 2;
                maxAnswers = 2;
            }
            default -> throw new IllegalArgumentException(location + ": el tipo de pregunta no está admitido.");
        }

        if (answersNode.size() < minAnswers || answersNode.size() > maxAnswers) {
            throw new IllegalArgumentException(location + ": el número de opciones no es válido para su tipo.");
        }

        ArrayList<OpcionRespuestaForm> answers = new ArrayList<>();
        int correctAnswers = 0;
        for (int index = 0; index < answersNode.size(); index++) {
            JsonNode answerNode = answersNode.get(index);
            String answerLocation = location + ", opción " + (index + 1);
            requireObject(answerNode, answerLocation + " debe ser un objeto.");
            rejectUnknownFields(answerNode, ANSWER_FIELDS, answerLocation);

            OpcionRespuestaForm answer = new OpcionRespuestaForm();
            answer.setTexto(requiredText(answerNode, "texto", answerLocation + ": el texto es obligatorio.", 255));

            JsonNode correctNode = answerNode.get("correcta");
            if (correctNode == null || !correctNode.isBoolean()) {
                throw new IllegalArgumentException(answerLocation + ": 'correcta' debe ser true o false.");
            }
            answer.setCorrecta(correctNode.booleanValue());
            if (answer.isCorrecta()) {
                correctAnswers++;
            }

            answer.setRetroalimentacion(optionalText(answerNode, "retroalimentacion",
                    answerLocation + ": la retroalimentación", 500));
            answers.add(answer);
        }

        if (correctAnswers != 1) {
            throw new IllegalArgumentException(location + ": debe haber exactamente una opción correcta.");
        }
        if ("TRUE_FALSE".equals(typeCode)
                && (!"Verdadero".equals(answers.get(0).getTexto())
                        || !"Falso".equals(answers.get(1).getTexto()))) {
            throw new IllegalArgumentException(location + ": las opciones deben ser 'Verdadero' y 'Falso', en ese orden.");
        }

        PreguntaForm question = new PreguntaForm();
        question.setEnunciado(enunciado);
        question.setTipoPreguntaCodigo(typeCode);
        question.setOpcionesRespuesta(answers);
        return question;
    }

    private String requiredText(JsonNode node, String field, String errorMessage, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asString().isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        String text = value.asString();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException("El campo '" + field + "' no puede superar los " + maxLength + " caracteres.");
        }
        return text;
    }

    private String optionalText(JsonNode node, String field, String label, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(label + " debe ser texto.");
        }
        String text = value.asString();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(label + " no puede superar los " + maxLength + " caracteres.");
        }
        return text;
    }

    private void requireObject(JsonNode node, String errorMessage) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void rejectUnknownFields(JsonNode node, Set<String> allowedFields, String location) {
        node.properties().forEach(entry -> {
            if (!allowedFields.contains(entry.getKey())) {
                throw new IllegalArgumentException("Campo no reconocido en " + location + ": '" + entry.getKey() + "'.");
            }
        });
    }
}