package com.quizly.app.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizly.app.dto.ai.MapaGenerado;
import com.quizly.app.dto.ai.TarjetaGenerada;
import com.quizly.app.exception.MapaMentalNotFoundException;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.MapaMental;
import com.quizly.app.model.entity.Mazo;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.view.mapa.ListadoMapas;
import com.quizly.app.model.view.mapa.ResultadoMazoDesdeMapa;
import com.quizly.app.repository.MapaMentalRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.util.TextoTarjeta;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class MapaMentalService {

    public static final int MAX_BYTES = 256 * 1024;
    public static final int MAX_NODOS = 500;
    public static final int MAX_PROFUNDIDAD = 10;
    public static final int MAX_TOPIC = 200;
    /** Mismo máximo por mazo que la generación con IA. */
    public static final int MAX_TARJETAS = 50;

    private static final String SEPARADOR_RUTA = " › ";
    private static final String ELIPSIS = "…";

    private static final String TOPIC_VACIO = "Sin título";
    // El id acaba en un selector del DOM de la librería: solo caracteres inertes
    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final MapaMentalRepository mapaMentalRepository;
    private final UsuarioRepository usuarioRepository;
    private final TemaRepository temaRepository;
    private final MazoService mazoService;
    private final ObjectMapper objectMapper;

    /** @param asignaturaId si no es {@code null}, solo los mapas cuyo tema es de esa asignatura */
    @Transactional(readOnly = true)
    public ListadoMapas listado(Long usuarioId, Long asignaturaId) {
        List<MapaMental> todos = mapaMentalRepository.findConTemaByUsuarioId(usuarioId);

        Map<Long, Asignatura> asignaturas = new LinkedHashMap<>();
        todos.stream().map(MapaMentalService::asignaturaDe).filter(a -> a != null)
                .forEach(a -> asignaturas.putIfAbsent(a.getId(), a));
        List<Asignatura> opciones = asignaturas.values().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre())).toList();

        List<MapaMental> mapas = asignaturaId == null ? todos
                : todos.stream().filter(m -> asignaturaDe(m) != null && asignaturaId.equals(asignaturaDe(m).getId()))
                        .toList();
        return new ListadoMapas(mapas, opciones, asignaturaId);
    }

    /** Los temas son globales (no de un usuario): se agrupan por asignatura para el {@code <select>} del editor. */
    @Transactional(readOnly = true)
    public Map<String, List<Tema>> temasPorAsignatura() {
        return temaRepository.findAllByOrderByNombreAsc().stream().collect(Collectors.groupingBy(
                t -> t.getAsignatura() != null ? t.getAsignatura().getNombre() : "Sin asignatura",
                () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Collectors.toList()));
    }

    /** Puerta de entrada de la propiedad: un mapa ajeno es indistinguible de uno inexistente. */
    @Transactional(readOnly = true)
    public MapaMental obtener(Long id, Long usuarioId) {
        return mapaMentalRepository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(MapaMentalNotFoundException::new);
    }

    @Transactional
    public MapaMental crear(Long usuarioId, String titulo, String descripcion, Long temaId) {
        String limpio = titulo.trim();
        return guardarNuevo(usuarioId, limpio, descripcion, buscarTema(temaId), contenidoInicial(limpio));
    }

    /** El árbol de la IA se revalida igual que el del editor: es entrada externa, aunque la pida un admin. */
    @Transactional
    public MapaMental crearDesdeIa(Long usuarioId, MapaGenerado generado) {
        return guardarNuevo(usuarioId, generado.titulo(), "Generado con IA a partir de un PDF.", null,
                normalizarContenido(generado.contenido()));
    }

    private MapaMental guardarNuevo(Long usuarioId, String titulo, String descripcion, Tema tema, String contenido) {
        MapaMental mapa = new MapaMental();
        mapa.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        mapa.setTema(tema);
        mapa.setTitulo(titulo);
        mapa.setDescripcion(limpiar(descripcion));
        mapa.setContenido(contenido);
        return mapaMentalRepository.save(mapa);
    }

    /**
     * Guarda título, descripción y árbol. El contenido llega del navegador, así que se revalida entera.
     *
     * @throws IllegalArgumentException si el contenido no cumple los límites o no es un árbol válido, o si el tema no existe
     */
    @Transactional
    public void guardar(Long id, Long usuarioId, String titulo, String descripcion, Long temaId, String contenido) {
        MapaMental mapa = obtener(id, usuarioId);
        String normalizado = normalizarContenido(contenido);
        Tema tema = buscarTema(temaId);
        mapa.setTema(tema);
        mapa.setTitulo(titulo.trim());
        mapa.setDescripcion(limpiar(descripcion));
        mapa.setContenido(normalizado);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        mapaMentalRepository.delete(obtener(id, usuarioId));
    }

    /**
     * Crea un mazo con una tarjeta por cada nodo que tiene hijos (la raíz incluida), en preorden:
     * el anverso es la ruta hasta el nodo ({@code Raíz › Rama › Nodo}) y el reverso sus hijos. Trabaja sobre
     * el contenido guardado. Se queda con las {@value #MAX_TARJETAS} primeras si el mapa da más.
     */
    @Transactional
    public ResultadoMazoDesdeMapa crearMazo(Long id, Long usuarioId) {
        MapaMental mapa = obtener(id, usuarioId);
        List<TarjetaGenerada> tarjetas = new ArrayList<>();
        recogerTarjetas(objectMapper.readTree(mapa.getContenido()).path("nodeData"), new ArrayList<>(), tarjetas);
        if (tarjetas.isEmpty()) {
            return new ResultadoMazoDesdeMapa(null, 0, 0);
        }

        List<TarjetaGenerada> aGuardar = List.copyOf(tarjetas.subList(0, Math.min(tarjetas.size(), MAX_TARJETAS)));
        Mazo mazo = mazoService.crearConTarjetas(usuarioId, mapa.getTitulo(),
                "Creado a partir del mapa mental «" + mapa.getTitulo() + "».", aGuardar);
        return new ResultadoMazoDesdeMapa(mazo.getId(), aGuardar.size(), tarjetas.size());
    }

    private void recogerTarjetas(JsonNode nodo, List<String> ruta, List<TarjetaGenerada> salida) {
        ruta.add(nodo.path("topic").asText(""));
        JsonNode hijos = nodo.path("children");
        if (hijos.isArray() && !hijos.isEmpty()) {
            List<String> temas = new ArrayList<>();
            hijos.forEach(h -> temas.add(h.path("topic").asText("")));
            salida.add(new TarjetaGenerada(anverso(ruta),
                    TextoTarjeta.normalizar(String.join("; ", temas), TextoTarjeta.MAX_REVERSO)));
            hijos.forEach(h -> recogerTarjetas(h, ruta, salida));
        }
        ruta.remove(ruta.size() - 1);
    }

    /** Ruta hasta el nodo; si no cabe se recorta por el principio, porque lo que se pregunta es el final. */
    private static String anverso(List<String> ruta) {
        String texto = TextoTarjeta.normalizar(String.join(SEPARADOR_RUTA, ruta), Integer.MAX_VALUE);
        if (texto.length() <= TextoTarjeta.MAX_ANVERSO) {
            return texto;
        }
        return ELIPSIS + texto.substring(texto.length() - (TextoTarjeta.MAX_ANVERSO - ELIPSIS.length())).stripLeading();
    }

    public String contenidoInicial(String titulo) {
        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("id", nuevoId());
        raiz.put("topic", recortar(titulo, MAX_TOPIC));
        raiz.set("children", objectMapper.createArrayNode());
        ObjectNode contenido = objectMapper.createObjectNode();
        contenido.set("nodeData", raiz);
        return objectMapper.writeValueAsString(contenido);
    }

    /**
     * Reconstruye el árbol con una lista blanca ({@code id}, {@code topic}, {@code children}, {@code expanded},
     * {@code direction}) en vez de filtrar lo peligroso: la librería del editor interpreta HTML en
     * {@code dangerouslySetInnerHTML}, enlaces en {@code hyperLink} y URL en {@code image}, y nada de eso
     * debe poder llegar a la base de datos.
     *
     * @throws IllegalArgumentException si supera los límites o no tiene la forma {@code {"nodeData": {...}}}
     */
    public String normalizarContenido(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El mapa está vacío.");
        }
        if (contenido.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("El mapa es demasiado grande (máximo " + MAX_BYTES / 1024 + " KB).");
        }

        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(contenido);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("El contenido del mapa no es válido.");
        }
        JsonNode nodeData = raiz.path("nodeData");
        if (!nodeData.isObject()) {
            throw new IllegalArgumentException("El contenido del mapa no es válido.");
        }

        ObjectNode resultado = objectMapper.createObjectNode();
        resultado.set("nodeData", copiarNodo(nodeData, 1, new int[1], new HashSet<>()));
        return objectMapper.writeValueAsString(resultado);
    }

    private ObjectNode copiarNodo(JsonNode origen, int profundidad, int[] contador, Set<String> ids) {
        if (profundidad > MAX_PROFUNDIDAD) {
            throw new IllegalArgumentException("El mapa tiene demasiados niveles (máximo " + MAX_PROFUNDIDAD + ").");
        }
        if (++contador[0] > MAX_NODOS) {
            throw new IllegalArgumentException("El mapa tiene demasiados nodos (máximo " + MAX_NODOS + ").");
        }

        String topic = origen.path("topic").asText("").trim();
        if (topic.length() > MAX_TOPIC) {
            throw new IllegalArgumentException("Un nodo supera los " + MAX_TOPIC + " caracteres.");
        }

        ObjectNode nodo = objectMapper.createObjectNode();
        nodo.put("id", idUnico(origen.path("id").asText(""), ids));
        nodo.put("topic", topic.isEmpty() ? TOPIC_VACIO : topic);

        JsonNode expanded = origen.path("expanded");
        if (expanded.isBoolean()) {
            nodo.put("expanded", expanded.booleanValue());
        }
        JsonNode direction = origen.path("direction");
        if (direction.isInt() && (direction.intValue() == 0 || direction.intValue() == 1)) {
            nodo.put("direction", direction.intValue());
        }

        JsonNode hijos = origen.path("children");
        if (hijos.isArray() && !hijos.isEmpty()) {
            ArrayNode copia = objectMapper.createArrayNode();
            for (JsonNode hijo : hijos) {
                if (hijo.isObject()) {
                    copia.add(copiarNodo(hijo, profundidad + 1, contador, ids));
                }
            }
            nodo.set("children", copia);
        }
        return nodo;
    }

    /** Conserva el id si es válido y no está repetido; si no, genera uno nuevo. */
    private String idUnico(String id, Set<String> usados) {
        String candidato = ID_VALIDO.matcher(id).matches() ? id : nuevoId();
        while (!usados.add(candidato)) {
            candidato = nuevoId();
        }
        return candidato;
    }

    /** @return el tema, o {@code null} si no se eligió ninguno */
    private Tema buscarTema(Long temaId) {
        if (temaId == null) {
            return null;
        }
        return temaRepository.findById(temaId)
                .orElseThrow(() -> new IllegalArgumentException("El tema seleccionado no existe."));
    }

    private static Asignatura asignaturaDe(MapaMental mapa) {
        return mapa.getTema() == null ? null : mapa.getTema().getAsignatura();
    }

    private static String nuevoId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
