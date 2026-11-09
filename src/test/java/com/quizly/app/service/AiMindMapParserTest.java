package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quizly.app.dto.ai.MapaGenerado;
import com.quizly.app.exception.AiGenerationException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class AiMindMapParserTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final AiQuizResponseParser parser = new AiQuizResponseParser(objectMapper);

    private String rawCompletion(String content) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("content", content), "finish_reason", "stop"))));
    }

    private JsonNode raiz(MapaGenerado mapa) {
        return objectMapper.readTree(mapa.contenido()).path("nodeData");
    }

    private int profundidad(JsonNode nodo) {
        int max = 0;
        for (JsonNode hijo : nodo.path("children")) {
            max = Math.max(max, profundidad(hijo));
        }
        return 1 + max;
    }

    /** Cadena raíz > n1 > n2 ... con {@code niveles} nodos en total. */
    private String cadena(int niveles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < niveles; i++) {
            sb.append("{\"texto\":\"n").append(i).append("\",\"hijos\":[");
        }
        sb.append("]}".repeat(niveles));
        return sb.toString();
    }

    @Test
    @DisplayName("Parsea un mapa válido, normaliza el texto y no inventa ids (los pone el servicio al guardar)")
    void parseaUnMapaValido() throws Exception {
        String json = """
                {"titulo": "  La   memoria ", "raiz": {"texto": "Memoria", "hijos": [
                    {"texto": "Declarativa", "hijos": [{"texto": "Episódica"}, {"texto": "  Semántica "}]},
                    {"texto": "Procedimental"}]}}""";

        MapaGenerado mapa = parser.parseMindMap(rawCompletion(json), "memoria.pdf");

        assertEquals("La memoria", mapa.titulo());
        assertEquals(5, mapa.nodos());
        JsonNode raiz = raiz(mapa);
        assertEquals("Memoria", raiz.path("topic").asText());
        assertEquals("Declarativa", raiz.path("children").get(0).path("topic").asText());
        assertEquals("Semántica", raiz.path("children").get(0).path("children").get(1).path("topic").asText());
        assertTrue(raiz.path("id").isMissingNode());
        assertTrue(raiz.path("children").get(1).path("children").isMissingNode()); // las hojas no llevan children vacío
    }

    @Test
    @DisplayName("Acepta la respuesta envuelta en un bloque de código markdown")
    void aceptaCercasDeCodigo() throws Exception {
        String json = "```json\n{\"titulo\":\"T\",\"raiz\":{\"texto\":\"R\",\"hijos\":[{\"texto\":\"A\"}]}}\n```";

        assertEquals(2, parser.parseMindMap(rawCompletion(json), "x.pdf").nodos());
    }

    @Test
    @DisplayName("Sin título usa el nombre del fichero; sin fichero, un título genérico")
    void tituloPorDefecto() throws Exception {
        String json = "{\"raiz\":{\"texto\":\"R\",\"hijos\":[{\"texto\":\"A\"}]}}";

        assertEquals("Mapa de apuntes", parser.parseMindMap(rawCompletion(json), "apuntes.PDF").titulo());
        assertEquals("Mapa generado por IA", parser.parseMindMap(rawCompletion(json), null).titulo());
    }

    @Test
    @DisplayName("Una raíz sin texto toma el título del mapa")
    void raizSinTexto() throws Exception {
        String json = "{\"titulo\":\"Mi mapa\",\"raiz\":{\"hijos\":[{\"texto\":\"A\"}]}}";

        assertEquals("Mi mapa", raiz(parser.parseMindMap(rawCompletion(json), "x.pdf")).path("topic").asText());
    }

    @Test
    @DisplayName("Lo que pasa de 4 niveles se descarta en vez de rechazar el mapa")
    void truncaLaProfundidad() throws Exception {
        String json = "{\"titulo\":\"T\",\"raiz\":" + cadena(8) + "}";

        MapaGenerado mapa = parser.parseMindMap(rawCompletion(json), "x.pdf");

        assertEquals(AiQuizResponseParser.MAPA_MAX_PROFUNDIDAD, profundidad(raiz(mapa)));
        assertEquals(AiQuizResponseParser.MAPA_MAX_PROFUNDIDAD, mapa.nodos());
    }

    @Test
    @DisplayName("Lo que pasa de 80 nodos se descarta y el contador coincide con el árbol")
    void truncaLosNodos() throws Exception {
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            hijos.append(i == 0 ? "" : ",").append("{\"texto\":\"rama ").append(i).append("\"}");
        }
        String json = "{\"titulo\":\"T\",\"raiz\":{\"texto\":\"R\",\"hijos\":[" + hijos + "]}}";

        MapaGenerado mapa = parser.parseMindMap(rawCompletion(json), "x.pdf");

        assertEquals(AiQuizResponseParser.MAPA_MAX_NODOS, mapa.nodos());
        assertEquals(AiQuizResponseParser.MAPA_MAX_NODOS - 1, raiz(mapa).path("children").size());
        assertEquals("rama 0", raiz(mapa).path("children").get(0).path("topic").asText()); // se conserva el principio
    }

    @Test
    @DisplayName("Omite los nodos sin texto y recorta los textos largos")
    void omiteVaciosYRecortaLargos() throws Exception {
        String largo = "x".repeat(500);
        String json = "{\"titulo\":\"T\",\"raiz\":{\"texto\":\"R\",\"hijos\":["
                + "{\"texto\":\"  \"},{\"hijos\":[{\"texto\":\"huérfano\"}]},{\"texto\":\"" + largo + "\"}]}}";

        MapaGenerado mapa = parser.parseMindMap(rawCompletion(json), "x.pdf");

        JsonNode hijos = raiz(mapa).path("children");
        assertEquals(1, hijos.size());
        assertEquals(MapaMentalService.MAX_TOPIC, hijos.get(0).path("topic").asText().length());
        assertEquals(2, mapa.nodos());
    }

    @Test
    @DisplayName("Sin ninguna rama, o con una respuesta que no es JSON, es un error")
    void rechazaMapasInvalidos() throws Exception {
        assertThrows(AiGenerationException.class,
                () -> parser.parseMindMap(rawCompletion("{\"titulo\":\"T\",\"raiz\":{\"texto\":\"R\",\"hijos\":[]}}"), "x.pdf"));
        assertThrows(AiGenerationException.class,
                () -> parser.parseMindMap(rawCompletion("{\"titulo\":\"T\",\"raiz\":{\"texto\":\"R\"}}"), "x.pdf"));
        assertThrows(AiGenerationException.class, () -> parser.parseMindMap(rawCompletion("esto no es json"), "x.pdf"));
    }

    @Test
    @DisplayName("El árbol que produce el parser supera la validación del servicio al guardarlo")
    void elResultadoPasaLaValidacionDelServicio() throws Exception {
        String json = "{\"titulo\":\"T\",\"raiz\":" + cadena(4) + "}";
        MapaGenerado mapa = parser.parseMindMap(rawCompletion(json), "x.pdf");

        // Sin ids: normalizarContenido los genera
        MapaMentalService servicio = new MapaMentalService(null, null, null, null, objectMapper);
        JsonNode normalizado = objectMapper.readTree(servicio.normalizarContenido(mapa.contenido())).path("nodeData");

        assertEquals(4, profundidad(normalizado));
        assertTrue(normalizado.path("id").asText().matches("[A-Za-z0-9_-]{1,64}"));
    }
}
