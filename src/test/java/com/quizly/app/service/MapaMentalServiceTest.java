package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quizly.app.dto.ai.MapaGenerado;
import com.quizly.app.dto.ai.TarjetaGenerada;
import com.quizly.app.exception.MapaMentalNotFoundException;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.MapaMental;
import com.quizly.app.model.entity.Mazo;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.view.mapa.ListadoMapas;
import com.quizly.app.model.view.mapa.ResultadoMazoDesdeMapa;
import com.quizly.app.repository.MapaMentalRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.repository.UsuarioRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class MapaMentalServiceTest {

    private static final Long USUARIO = 7L;
    private static final Long MAPA = 1L;

    @Mock
    private MapaMentalRepository mapaMentalRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TemaRepository temaRepository;
    @Mock
    private MazoService mazoService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private MapaMentalService service;

    @BeforeEach
    void preparar() {
        service = new MapaMentalService(mapaMentalRepository, usuarioRepository, temaRepository, mazoService, objectMapper);
    }

    private JsonNode normalizar(String json) {
        return objectMapper.readTree(service.normalizarContenido(json)).path("nodeData");
    }

    /** Cadena de {@code niveles} nodos anidados: raíz > hijo > nieto... */
    private String cadena(int niveles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < niveles; i++) {
            sb.append("{\"id\":\"n").append(i).append("\",\"topic\":\"t\",\"children\":[");
        }
        sb.append("]}".repeat(niveles));
        return "{\"nodeData\":" + sb + "}";
    }

    // =========================================================================
    // Propiedad
    // =========================================================================

    @Test
    @DisplayName("Un mapa ajeno o inexistente se trata igual: 404 al obtener, guardar y eliminar")
    void mapaAjeno() {
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.empty());

        assertThrows(MapaMentalNotFoundException.class, () -> service.obtener(MAPA, USUARIO));
        assertThrows(MapaMentalNotFoundException.class,
                () -> service.guardar(MAPA, USUARIO, "t", null, null, "{\"nodeData\":{\"id\":\"a\",\"topic\":\"x\"}}"));
        assertThrows(MapaMentalNotFoundException.class, () -> service.eliminar(MAPA, USUARIO));
    }

    // =========================================================================
    // normalizarContenido
    // =========================================================================

    @Test
    @DisplayName("Descarta todo lo que no esté en la lista blanca (HTML, enlaces, imágenes, estilos...)")
    void descartaCamposPeligrosos() {
        JsonNode nodo = normalizar("""
                {"nodeData":{"id":"raiz","topic":"Raíz","style":{"color":"red"},
                  "dangerouslySetInnerHTML":"<img src=x onerror=alert(1)>",
                  "hyperLink":"javascript:alert(1)","image":{"url":"http://x/y.png","width":1,"height":1},
                  "tags":["a"],"icons":["b"],"children":[
                    {"id":"h1","topic":"Hijo","hyperLink":"javascript:alert(2)","extra":42}]},
                 "arrows":[{"from":"a","to":"b"}],"summaries":[{"id":"s"}]}
                """);

        assertEquals("Raíz", nodo.path("topic").asText());
        for (String prohibido : new String[] { "style", "dangerouslySetInnerHTML", "hyperLink", "image", "tags", "icons" }) {
            assertTrue(nodo.path(prohibido).isMissingNode(), prohibido);
        }
        JsonNode hijo = nodo.path("children").get(0);
        assertTrue(hijo.path("hyperLink").isMissingNode());
        assertTrue(hijo.path("extra").isMissingNode());
        // Solo se conserva nodeData
        assertTrue(objectMapper.readTree(service.normalizarContenido(
                "{\"nodeData\":{\"id\":\"a\",\"topic\":\"x\"},\"arrows\":[1]}")).path("arrows").isMissingNode());
    }

    @Test
    @DisplayName("Conserva expanded y direction válidos y descarta los de tipo o valor incorrecto")
    void conservaEstadoDeVista() {
        JsonNode nodo = normalizar("""
                {"nodeData":{"id":"r","topic":"R","children":[
                  {"id":"a","topic":"A","expanded":false,"direction":0},
                  {"id":"b","topic":"B","expanded":"no","direction":7}]}}
                """);

        JsonNode a = nodo.path("children").get(0);
        assertFalse(a.path("expanded").booleanValue());
        assertTrue(a.path("expanded").isBoolean());
        assertEquals(0, a.path("direction").intValue());
        JsonNode b = nodo.path("children").get(1);
        assertTrue(b.path("expanded").isMissingNode());
        assertTrue(b.path("direction").isMissingNode());
    }

    @Test
    @DisplayName("Un topic con HTML se conserva como texto: la librería lo pinta con textContent")
    void topicConHtmlSeConservaComoTexto() {
        JsonNode nodo = normalizar("{\"nodeData\":{\"id\":\"a\",\"topic\":\"  <img src=x onerror=alert(1)>  \"}}");

        assertEquals("<img src=x onerror=alert(1)>", nodo.path("topic").asText());
    }

    @Test
    @DisplayName("Un nodo sin texto queda como 'Sin título' en vez de perder la estructura")
    void topicVacio() {
        JsonNode nodo = normalizar("{\"nodeData\":{\"id\":\"a\",\"topic\":\"   \",\"children\":[{\"id\":\"b\"}]}}");

        assertEquals("Sin título", nodo.path("topic").asText());
        assertEquals("Sin título", nodo.path("children").get(0).path("topic").asText());
    }

    @Test
    @DisplayName("Los ids repetidos o con caracteres raros se regeneran; los válidos se conservan")
    void idsUnicosYSeguros() {
        JsonNode nodo = normalizar("""
                {"nodeData":{"id":"raiz","topic":"R","children":[
                  {"id":"raiz","topic":"duplicado"},
                  {"id":"a\\"]; alert(1)//","topic":"raro"},
                  {"topic":"sin id"}]}}
                """);

        String raiz = nodo.path("id").asText();
        assertEquals("raiz", raiz);
        java.util.Set<String> ids = new java.util.HashSet<>();
        ids.add(raiz);
        for (JsonNode hijo : nodo.path("children")) {
            String id = hijo.path("id").asText();
            assertTrue(id.matches("[A-Za-z0-9_-]{1,64}"), id);
            assertTrue(ids.add(id), "id repetido: " + id);
        }
        assertNotEquals("raiz", nodo.path("children").get(0).path("id").asText());
    }

    @Test
    @DisplayName("Rechaza más de 500 nodos")
    void demasiadosNodos() {
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < MapaMentalService.MAX_NODOS; i++) { // + la raíz = 501
            hijos.append(i == 0 ? "" : ",").append("{\"id\":\"h").append(i).append("\",\"topic\":\"x\"}");
        }
        String json = "{\"nodeData\":{\"id\":\"r\",\"topic\":\"R\",\"children\":[" + hijos + "]}}";

        assertThrows(IllegalArgumentException.class, () -> service.normalizarContenido(json));
    }

    @Test
    @DisplayName("Acepta exactamente 500 nodos")
    void justoElMaximoDeNodos() {
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < MapaMentalService.MAX_NODOS - 1; i++) { // + la raíz = 500
            hijos.append(i == 0 ? "" : ",").append("{\"id\":\"h").append(i).append("\",\"topic\":\"x\"}");
        }
        String json = "{\"nodeData\":{\"id\":\"r\",\"topic\":\"R\",\"children\":[" + hijos + "]}}";

        assertEquals(MapaMentalService.MAX_NODOS - 1, normalizar(json).path("children").size());
    }

    @Test
    @DisplayName("Rechaza más de 10 niveles y acepta justo 10")
    void profundidad() {
        assertEquals("n0", normalizar(cadena(MapaMentalService.MAX_PROFUNDIDAD)).path("id").asText());
        assertThrows(IllegalArgumentException.class,
                () -> service.normalizarContenido(cadena(MapaMentalService.MAX_PROFUNDIDAD + 1)));
    }

    @Test
    @DisplayName("Rechaza un topic de más de 200 caracteres")
    void topicLargo() {
        String json = "{\"nodeData\":{\"id\":\"a\",\"topic\":\"" + "x".repeat(MapaMentalService.MAX_TOPIC + 1) + "\"}}";

        assertThrows(IllegalArgumentException.class, () -> service.normalizarContenido(json));
    }

    @Test
    @DisplayName("Rechaza un contenido de más de 256 KB")
    void contenidoDemasiadoGrande() {
        String json = "{\"nodeData\":{\"id\":\"a\",\"topic\":\"x\"},\"relleno\":\"" + "y".repeat(MapaMentalService.MAX_BYTES) + "\"}";

        assertThrows(IllegalArgumentException.class, () -> service.normalizarContenido(json));
    }

    @Test
    @DisplayName("Rechaza JSON inválido, vacío, sin nodeData o con nodeData que no es un objeto")
    void contenidoInvalido() {
        for (String malo : new String[] { null, "", "   ", "no es json", "{\"otra\":1}", "{\"nodeData\":[]}",
                "{\"nodeData\":\"x\"}", "[1,2]" }) {
            assertThrows(IllegalArgumentException.class, () -> service.normalizarContenido(malo), String.valueOf(malo));
        }
    }

    // =========================================================================
    // crear / guardar
    // =========================================================================

    @Test
    @DisplayName("Crear deja un mapa con la raíz titulada como el mapa y sin hijos")
    void crearConRaizInicial() {
        when(usuarioRepository.getReferenceById(USUARIO)).thenReturn(new Usuario());
        when(mapaMentalRepository.save(any(MapaMental.class))).thenAnswer(i -> i.getArgument(0));

        MapaMental mapa = service.crear(USUARIO, "  La memoria ", "   ", null);

        assertEquals("La memoria", mapa.getTitulo());
        assertNull(mapa.getDescripcion());
        JsonNode raiz = objectMapper.readTree(mapa.getContenido()).path("nodeData");
        assertEquals("La memoria", raiz.path("topic").asText());
        assertTrue(raiz.path("children").isEmpty());
    }

    @Test
    @DisplayName("Guardar persiste el contenido normalizado, no el recibido")
    void guardarNormaliza() {
        MapaMental mapa = new MapaMental();
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.of(mapa));

        service.guardar(MAPA, USUARIO, " Nuevo título ", " desc ", null,
                "{\"nodeData\":{\"id\":\"a\",\"topic\":\"x\",\"hyperLink\":\"javascript:alert(1)\"}}");

        assertEquals("Nuevo título", mapa.getTitulo());
        assertEquals("desc", mapa.getDescripcion());
        assertFalse(mapa.getContenido().contains("hyperLink"));
        assertTrue(mapa.getContenido().contains("\"topic\":\"x\""));
    }

    @Test
    @DisplayName("Si el contenido no es válido, el mapa no se modifica")
    void guardarInvalidoNoToca() {
        MapaMental mapa = new MapaMental();
        mapa.setTitulo("Original");
        mapa.setContenido("original");
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.of(mapa));

        assertThrows(IllegalArgumentException.class, () -> service.guardar(MAPA, USUARIO, "Otro", null, null, "basura"));

        assertEquals("Original", mapa.getTitulo());
        assertEquals("original", mapa.getContenido());
    }

    // =========================================================================
    // Tema, listado y agrupación
    // =========================================================================

    private static final String CONTENIDO_VALIDO = "{\"nodeData\":{\"id\":\"a\",\"topic\":\"x\"}}";

    private Tema tema(Long id, String nombre, String asignatura, Long asignaturaId) {
        Tema tema = new Tema();
        tema.setId(id);
        tema.setNombre(nombre);
        if (asignatura != null) {
            Asignatura a = new Asignatura();
            a.setId(asignaturaId);
            a.setNombre(asignatura);
            tema.setAsignatura(a);
        }
        return tema;
    }

    private MapaMental mapa(String titulo, Tema tema) {
        MapaMental mapa = new MapaMental();
        mapa.setTitulo(titulo);
        mapa.setTema(tema);
        return mapa;
    }

    @Test
    @DisplayName("Guardar asigna el tema elegido y con null lo quita")
    void guardarConYSinTema() {
        MapaMental mapa = new MapaMental();
        Tema tema = tema(3L, "Memoria", "Psicología", 1L);
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.of(mapa));
        when(temaRepository.findById(3L)).thenReturn(Optional.of(tema));

        service.guardar(MAPA, USUARIO, "t", null, 3L, CONTENIDO_VALIDO);
        assertEquals(tema, mapa.getTema());

        service.guardar(MAPA, USUARIO, "t", null, null, CONTENIDO_VALIDO);
        assertNull(mapa.getTema());
    }

    @Test
    @DisplayName("Un tema inexistente se rechaza y el mapa no se modifica")
    void guardarConTemaInexistente() {
        MapaMental mapa = new MapaMental();
        mapa.setTitulo("Original");
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.of(mapa));
        when(temaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.guardar(MAPA, USUARIO, "Otro", null, 99L, CONTENIDO_VALIDO));
        assertThrows(IllegalArgumentException.class, () -> service.crear(USUARIO, "Nuevo", null, 99L));

        assertEquals("Original", mapa.getTitulo());
        assertNull(mapa.getContenido());
    }

    @Test
    @DisplayName("Crear con tema lo asocia al mapa")
    void crearConTema() {
        Tema tema = tema(3L, "Memoria", "Psicología", 1L);
        when(usuarioRepository.getReferenceById(USUARIO)).thenReturn(new Usuario());
        when(temaRepository.findById(3L)).thenReturn(Optional.of(tema));
        when(mapaMentalRepository.save(any(MapaMental.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(tema, service.crear(USUARIO, "Mapa", null, 3L).getTema());
    }

    @Test
    @DisplayName("Los temas se agrupan por asignatura sin distinguir mayúsculas y los huérfanos van aparte")
    void temasPorAsignatura() {
        when(temaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                tema(1L, "Atención", "psicología", 1L),
                tema(2L, "Memoria", "Psicología", 1L),
                tema(3L, "Álgebra", "Matemáticas", 2L),
                tema(4L, "Suelto", null, null)));

        Map<String, List<Tema>> grupos = service.temasPorAsignatura();

        assertEquals(List.of("Matemáticas", "psicología", "Sin asignatura"), List.copyOf(grupos.keySet()));
        assertEquals(1, grupos.get("Sin asignatura").size());
    }

    @Test
    @DisplayName("El listado ofrece las asignaturas de los mapas y filtra por la elegida")
    void listadoFiltraPorAsignatura() {
        MapaMental psico = mapa("Memoria", tema(1L, "Memoria", "Psicología", 10L));
        MapaMental mates = mapa("Límites", tema(2L, "Cálculo", "Matemáticas", 20L));
        MapaMental otroPsico = mapa("Atención", tema(3L, "Atención", "Psicología", 10L));
        MapaMental sinTema = mapa("Libre", null);
        when(mapaMentalRepository.findConTemaByUsuarioId(USUARIO))
                .thenReturn(List.of(psico, mates, otroPsico, sinTema));

        ListadoMapas todos = service.listado(USUARIO, null);
        assertEquals(4, todos.mapas().size());
        assertEquals(List.of("Matemáticas", "Psicología"),
                todos.asignaturas().stream().map(Asignatura::getNombre).toList());

        ListadoMapas filtrado = service.listado(USUARIO, 10L);
        assertEquals(List.of(psico, otroPsico), filtrado.mapas());
        // El filtro no recorta las opciones: se puede cambiar de asignatura
        assertEquals(2, filtrado.asignaturas().size());
        assertEquals(10L, filtrado.asignaturaId());
    }

    // =========================================================================
    // Crear mazo desde un mapa
    // =========================================================================

    /** JSON de un nodo con sus hijos ya serializados. */
    private String nodo(String topic, String... hijos) {
        return "{\"id\":\"n\",\"topic\":\"" + topic + "\",\"children\":[" + String.join(",", hijos) + "]}";
    }

    private void mapaGuardado(String titulo, String arbol) {
        MapaMental mapa = new MapaMental();
        mapa.setTitulo(titulo);
        mapa.setContenido("{\"nodeData\":" + arbol + "}");
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.of(mapa));
        Mazo mazo = new Mazo();
        mazo.setId(9L);
        // lenient: el caso 'sin ramas' comprueba justamente que no se llama
        lenient().when(mazoService.crearConTarjetas(any(), any(), any(), any())).thenReturn(mazo);
    }

    @SuppressWarnings("unchecked")
    private List<TarjetaGenerada> tarjetasCreadas() {
        ArgumentCaptor<List<TarjetaGenerada>> captor = ArgumentCaptor.forClass(List.class);
        verify(mazoService).crearConTarjetas(any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Cada nodo con hijos da una tarjeta, raíz incluida y en preorden; las hojas no")
    void mazoUnaTarjetaPorNodoConHijos() {
        mapaGuardado("Memoria", nodo("Memoria",
                nodo("Declarativa", nodo("Episódica"), nodo("Semántica")),
                nodo("Procedimental")));

        ResultadoMazoDesdeMapa resultado = service.crearMazo(MAPA, USUARIO);

        assertEquals(new ResultadoMazoDesdeMapa(9L, 2, 2), resultado);
        assertEquals(List.of(
                new TarjetaGenerada("Memoria", "Declarativa; Procedimental"),
                new TarjetaGenerada("Memoria › Declarativa", "Episódica; Semántica")), tarjetasCreadas());
    }

    @Test
    @DisplayName("El mazo lleva el título del mapa y una descripción que lo cita")
    void mazoConTituloDelMapa() {
        mapaGuardado("Mi mapa", nodo("R", nodo("A")));

        service.crearMazo(MAPA, USUARIO);

        verify(mazoService).crearConTarjetas(USUARIO, "Mi mapa", "Creado a partir del mapa mental «Mi mapa».",
                List.of(new TarjetaGenerada("R", "A")));
    }

    @Test
    @DisplayName("Un mapa sin ramas no crea ningún mazo")
    void mazoSinRamas() {
        mapaGuardado("Solo raíz", nodo("R"));

        ResultadoMazoDesdeMapa resultado = service.crearMazo(MAPA, USUARIO);

        assertNull(resultado.mazoId());
        verify(mazoService, never()).crearConTarjetas(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Con más de 50 tarjetas se guardan las 50 primeras y el resultado indica cuántas daba el mapa")
    void mazoTruncadoA50() {
        StringBuilder ramas = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            ramas.append(i == 0 ? "" : ",").append(nodo("rama" + i, nodo("hoja" + i)));
        }
        mapaGuardado("Grande", "{\"id\":\"r\",\"topic\":\"R\",\"children\":[" + ramas + "]}");

        ResultadoMazoDesdeMapa resultado = service.crearMazo(MAPA, USUARIO);

        assertEquals(MapaMentalService.MAX_TARJETAS, resultado.creadas());
        assertEquals(61, resultado.total()); // la raíz + 60 ramas
        List<TarjetaGenerada> guardadas = tarjetasCreadas();
        assertEquals(MapaMentalService.MAX_TARJETAS, guardadas.size());
        assertEquals("R", guardadas.get(0).anverso()); // el orden se conserva: se pierde el final, no el principio
        assertEquals("R › rama48", guardadas.get(49).anverso());
    }

    @Test
    @DisplayName("Una ruta de más de 500 caracteres se recorta por el principio para conservar el nodo")
    void anversoLargoSeRecortaPorElPrincipio() {
        String largo = "x".repeat(MapaMentalService.MAX_TOPIC);
        mapaGuardado("Profundo", nodo(largo + "1", nodo(largo + "2", nodo(largo + "3", nodo("Final", nodo("hijo"))))));

        service.crearMazo(MAPA, USUARIO);

        String anverso = tarjetasCreadas().get(3).anverso();
        assertEquals(500, anverso.length());
        assertTrue(anverso.startsWith("…"));
        assertTrue(anverso.endsWith(" › Final"));
    }

    @Test
    @DisplayName("Un reverso de más de 1000 caracteres se recorta por el final")
    void reversoLargoSeRecortaPorElFinal() {
        String[] hijos = new String[8];
        for (int i = 0; i < hijos.length; i++) {
            hijos[i] = nodo(i + "y".repeat(MapaMentalService.MAX_TOPIC - 1));
        }
        mapaGuardado("Ancho", nodo("R", hijos));

        service.crearMazo(MAPA, USUARIO);

        String reverso = tarjetasCreadas().get(0).reverso();
        assertEquals(1000, reverso.length());
        assertTrue(reverso.endsWith("…"));
        assertTrue(reverso.startsWith("0yyy"));
    }

    @Test
    @DisplayName("Crear mazo de un mapa ajeno da 404 y no crea nada")
    void mazoDeMapaAjeno() {
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.empty());

        assertThrows(MapaMentalNotFoundException.class, () -> service.crearMazo(MAPA, USUARIO));
        verify(mazoService, never()).crearConTarjetas(any(), any(), any(), any());
    }

    // =========================================================================
    // Crear desde IA
    // =========================================================================

    @Test
    @DisplayName("Crear desde IA guarda el árbol normalizado (con ids) como mapa del usuario, sin tema")
    void crearDesdeIa() {
        when(usuarioRepository.getReferenceById(USUARIO)).thenReturn(new Usuario());
        when(mapaMentalRepository.save(any(MapaMental.class))).thenAnswer(i -> i.getArgument(0));

        MapaMental mapa = service.crearDesdeIa(USUARIO, new MapaGenerado("La memoria",
                "{\"nodeData\":{\"topic\":\"Memoria\",\"children\":[{\"topic\":\"Declarativa\"}]}}", 2));

        assertEquals("La memoria", mapa.getTitulo());
        assertEquals("Generado con IA a partir de un PDF.", mapa.getDescripcion());
        assertNull(mapa.getTema());
        JsonNode raiz = objectMapper.readTree(mapa.getContenido()).path("nodeData");
        assertTrue(raiz.path("id").asText().matches("[A-Za-z0-9_-]{1,64}"));
        assertTrue(raiz.path("children").get(0).path("id").asText().matches("[A-Za-z0-9_-]{1,64}"));
        assertNotEquals(raiz.path("id").asText(), raiz.path("children").get(0).path("id").asText());
    }

    @Test
    @DisplayName("Un árbol de la IA que no pasa la validación no se guarda")
    void crearDesdeIaInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearDesdeIa(USUARIO, new MapaGenerado("T", "{\"otra\":1}", 1)));

        verify(mapaMentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar borra el mapa del propietario")
    void eliminar() {
        MapaMental mapa = new MapaMental();
        when(mapaMentalRepository.findByIdAndUsuarioId(MAPA, USUARIO)).thenReturn(Optional.of(mapa));

        service.eliminar(MAPA, USUARIO);

        ArgumentCaptor<MapaMental> borrado = ArgumentCaptor.forClass(MapaMental.class);
        verify(mapaMentalRepository).delete(borrado.capture());
        assertEquals(mapa, borrado.getValue());
    }
}
