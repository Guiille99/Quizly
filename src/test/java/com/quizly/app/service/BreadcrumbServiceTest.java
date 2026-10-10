package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.view.breadcrumb.BreadcrumbItem;

class BreadcrumbServiceTest {

    private BreadcrumbService breadcrumbService;

    @BeforeEach
    void setUp() {
        breadcrumbService = new BreadcrumbService();
    }

    @Test
    @DisplayName("asignaturas() devuelve solo el nodo raíz Asignaturas")
    void testAsignaturas() {
        List<BreadcrumbItem> items = breadcrumbService.asignaturas();
        assertEquals(1, items.size());
        assertEquals("Asignaturas", items.get(0).label());
        assertNull(items.get(0).url());
    }

    @Test
    @DisplayName("asignatura() devuelve Asignaturas y el nombre de la asignatura")
    void testAsignatura() {
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Matemáticas");
        asignatura.setSlug("matematicas");

        List<BreadcrumbItem> items = breadcrumbService.asignatura(asignatura);
        assertEquals(2, items.size());
        assertEquals("Asignaturas", items.get(0).label());
        assertEquals("/asignaturas", items.get(0).url());
        assertEquals("Matemáticas", items.get(1).label());
        assertNull(items.get(1).url());
    }

    @Test
    @DisplayName("temaAsignatura() devuelve Asignaturas > Asignatura > Tema")
    void testTemaAsignatura() {
        Asignatura asignatura = new Asignatura();
        asignatura.setNombre("Matemáticas");
        asignatura.setSlug("matematicas");

        Tema tema = new Tema();
        tema.setNombre("Álgebra");
        tema.setSlug("algebra");
        tema.setAsignatura(asignatura);

        List<BreadcrumbItem> items = breadcrumbService.temaAsignatura(tema);
        assertEquals(3, items.size());
        assertEquals("Asignaturas", items.get(0).label());
        assertEquals("/asignaturas", items.get(0).url());
        assertEquals("Matemáticas", items.get(1).label());
        assertEquals("/asignaturas/matematicas", items.get(1).url());
        assertEquals("Álgebra", items.get(2).label());
        assertNull(items.get(2).url());
    }

    @Test
    @DisplayName("cuestionariosTodos() y cuestionariosRecientes() devuelven la estructura correcta")
    void testCuestionarios() {
        List<BreadcrumbItem> todos = breadcrumbService.cuestionariosTodos();
        assertEquals(1, todos.size());
        assertEquals("Cuestionarios", todos.get(0).label());
        assertNull(todos.get(0).url());

        List<BreadcrumbItem> recientes = breadcrumbService.cuestionariosRecientes();
        assertEquals(2, recientes.size());
        assertEquals("Cuestionarios", recientes.get(0).label());
        assertEquals("/cuestionarios", recientes.get(0).url());
        assertEquals("Recientes", recientes.get(1).label());
        assertNull(recientes.get(1).url());
    }

    @Test
    @DisplayName("misCuestionarios(), nuevo y editar devuelven la jerarquía esperada")
    void testMisCuestionarios() {
        List<BreadcrumbItem> list = breadcrumbService.misCuestionarios();
        assertEquals(2, list.size());
        assertEquals("Cuestionarios", list.get(0).label());
        assertEquals("Mis cuestionarios", list.get(1).label());

        List<BreadcrumbItem> nuevo = breadcrumbService.misCuestionariosNuevo();
        assertEquals(2, nuevo.size());
        assertEquals("Mis cuestionarios", nuevo.get(0).label());
        assertEquals("/mis-cuestionarios", nuevo.get(0).url());
        assertEquals("Nuevo cuestionario", nuevo.get(1).label());
        assertNull(nuevo.get(1).url());

        List<BreadcrumbItem> editar = breadcrumbService.misCuestionariosEditar("Quiz 1");
        assertEquals(2, editar.size());
        assertEquals("Mis cuestionarios", editar.get(0).label());
        assertEquals("/mis-cuestionarios", editar.get(0).url());
        assertEquals("Editar 'Quiz 1'", editar.get(1).label());
        assertNull(editar.get(1).url());
    }

    @Test
    @DisplayName("miProgreso() devuelve Mi progreso")
    void testMiProgreso() {
        List<BreadcrumbItem> items = breadcrumbService.miProgreso();
        assertEquals(1, items.size());
        assertEquals("Mi progreso", items.get(0).label());
        assertNull(items.get(0).url());
    }

    @Test
    @DisplayName("adminDashboard() devuelve Administración > Dashboard")
    void testAdminDashboard() {
        List<BreadcrumbItem> items = breadcrumbService.adminDashboard();
        assertEquals(2, items.size());
        assertEquals("Administración", items.get(0).label());
        assertEquals("/admin", items.get(0).url());
        assertEquals("Dashboard", items.get(1).label());
        assertNull(items.get(1).url());
    }

    @Test
    @DisplayName("Rutas de administración de Asignaturas, Temas, Cuestionarios y Usuarios")
    void testAdminRoutes() {
        // Asignaturas
        assertEquals(2, breadcrumbService.adminAsignaturas().size());
        assertEquals(3, breadcrumbService.adminAsignaturaNuevo().size());
        assertEquals(3, breadcrumbService.adminAsignaturaEditar("Física").size());
        assertEquals("Editar 'Física'", breadcrumbService.adminAsignaturaEditar("Física").get(2).label());

        // Temas
        assertEquals(2, breadcrumbService.adminTemas().size());
        assertEquals(3, breadcrumbService.adminTemaNuevo().size());
        assertEquals(3, breadcrumbService.adminTemaEditar("Cinemática").size());
        assertEquals("Editar 'Cinemática'", breadcrumbService.adminTemaEditar("Cinemática").get(2).label());

        // Cuestionarios
        assertEquals(2, breadcrumbService.adminCuestionarios().size());
        assertEquals(3, breadcrumbService.adminCuestionarioNuevo().size());
        assertEquals(3, breadcrumbService.adminCuestionarioEditar("Examen Final").size());
        assertEquals("Editar 'Examen Final'", breadcrumbService.adminCuestionarioEditar("Examen Final").get(2).label());

        // Usuarios
        assertEquals(2, breadcrumbService.adminUsuarios().size());
        assertEquals(3, breadcrumbService.adminUsuarioNuevo().size());
        assertEquals(3, breadcrumbService.adminUsuarioEditar("admin1").size());
        assertEquals("Editar 'admin1'", breadcrumbService.adminUsuarioEditar("admin1").get(2).label());
    }
}

