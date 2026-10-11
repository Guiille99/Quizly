package com.quizly.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.view.breadcrumb.BreadcrumbItem;

@Service
public class BreadcrumbService {

    private static final BreadcrumbItem ADMIN = new BreadcrumbItem("Administración", "/admin");
    private static final BreadcrumbItem ASIGNATURAS = new BreadcrumbItem("Asignaturas", "/asignaturas");
    private static final BreadcrumbItem CUESTIONARIOS = new BreadcrumbItem("Cuestionarios", "/cuestionarios");
    private static final BreadcrumbItem MIS_CUESTIONARIOS = new BreadcrumbItem("Mis cuestionarios", "/mis-cuestionarios");

    // =========================================================================
    // Zona Estudiante: Asignaturas
    // =========================================================================

    /**
     * Ej: Asignaturas
     */
    public List<BreadcrumbItem> asignaturas() {
        return List.of(
            new BreadcrumbItem("Asignaturas", null)
        );
    }

    /**
     * Ej: Asignaturas > [Nombre asignatura]
     */
    public List<BreadcrumbItem> asignatura(Asignatura asignatura) {
        String nombre = asignatura != null ? asignatura.getNombre() : "";
        return List.of(
            ASIGNATURAS,
            new BreadcrumbItem(nombre, null)
        );
    }

    /**
     * Ej: Asignaturas > [Nombre asignatura] > [Nombre tema]
     */
    public List<BreadcrumbItem> temaAsignatura(Tema tema) {
        if (tema == null) {
            return asignaturas();
        }
        String nombreAsignatura = tema.getAsignatura() != null ? tema.getAsignatura().getNombre() : "";
        String slugAsignatura = tema.getAsignatura() != null ? tema.getAsignatura().getSlug() : "";
        return List.of(
            ASIGNATURAS,
            new BreadcrumbItem(nombreAsignatura, "/asignaturas/" + slugAsignatura),
            new BreadcrumbItem(tema.getNombre(), null)
        );
    }

    // =========================================================================
    // Zona Estudiante: Cuestionarios
    // =========================================================================

    /**
     * Ej: Cuestionarios
     */
    public List<BreadcrumbItem> cuestionariosTodos() {
        return List.of(
            new BreadcrumbItem("Cuestionarios", null)
        );
    }

    /**
     * Ej: Cuestionarios > Recientes
     */
    public List<BreadcrumbItem> cuestionariosRecientes() {
        return List.of(
            CUESTIONARIOS,
            new BreadcrumbItem("Recientes", null)
        );
    }

    // =========================================================================
    // Zona Estudiante: Mis Cuestionarios
    // =========================================================================

    /**
     * Ej: Cuestionarios > Mis cuestionarios
     */
    public List<BreadcrumbItem> misCuestionarios() {
        return List.of(
            CUESTIONARIOS,
            new BreadcrumbItem("Mis cuestionarios", null)
        );
    }

    /**
     * Ej: Cuestionarios > Mis cuestionarios > Nuevo cuestionario
     */
    public List<BreadcrumbItem> misCuestionariosNuevo() {
        return List.of(
            CUESTIONARIOS,
            MIS_CUESTIONARIOS,
            new BreadcrumbItem("Nuevo cuestionario", null)
        );
    }

    /**
     * Ej: Cuestionarios > Mis cuestionarios > Editar '[Título]'
     */
    public List<BreadcrumbItem> misCuestionariosEditar(String titulo) {
        String label = (titulo != null && !titulo.isBlank()) ? "Editar '" + titulo + "'" : "Editar cuestionario";
        return List.of(
            CUESTIONARIOS,
            MIS_CUESTIONARIOS,
            new BreadcrumbItem(label, null)
        );
    }

    // =========================================================================
    // Zona Estudiante: Mi Progreso
    // =========================================================================

    /**
     * Ej: Mi progreso
     */
    public List<BreadcrumbItem> miProgreso() {
        return List.of(
            new BreadcrumbItem("Mi progreso", null)
        );
    }

    // =========================================================================
    // Zona Administración: Dashboard
    // =========================================================================

    /**
     * Ej: Administración > Dashboard
     */
    public List<BreadcrumbItem> adminDashboard() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Dashboard", null)
        );
    }

    // =========================================================================
    // Zona Administración: Asignaturas
    // =========================================================================

    /**
     * Ej: Administración > Asignaturas
     */
    public List<BreadcrumbItem> adminAsignaturas() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Asignaturas", null)
        );
    }

    /**
     * Ej: Administración > Asignaturas > Nueva asignatura
     */
    public List<BreadcrumbItem> adminAsignaturaNuevo() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Asignaturas", "/admin/asignaturas"),
            new BreadcrumbItem("Nueva asignatura", null)
        );
    }

    /**
     * Ej: Administración > Asignaturas > Editar '[Nombre]'
     */
    public List<BreadcrumbItem> adminAsignaturaEditar(String nombre) {
        String label = (nombre != null && !nombre.isBlank()) ? "Editar '" + nombre + "'" : "Editar asignatura";
        return List.of(
            ADMIN,
            new BreadcrumbItem("Asignaturas", "/admin/asignaturas"),
            new BreadcrumbItem(label, null)
        );
    }

    // =========================================================================
    // Zona Administración: Temas
    // =========================================================================

    /**
     * Ej: Administración > Temas
     */
    public List<BreadcrumbItem> adminTemas() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Temas", null)
        );
    }

    /**
     * Ej: Administración > Temas > Nuevo tema
     */
    public List<BreadcrumbItem> adminTemaNuevo() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Temas", "/admin/temas"),
            new BreadcrumbItem("Nuevo tema", null)
        );
    }

    /**
     * Ej: Administración > Temas > Editar '[Nombre]'
     */
    public List<BreadcrumbItem> adminTemaEditar(String nombre) {
        String label = (nombre != null && !nombre.isBlank()) ? "Editar '" + nombre + "'" : "Editar tema";
        return List.of(
            ADMIN,
            new BreadcrumbItem("Temas", "/admin/temas"),
            new BreadcrumbItem(label, null)
        );
    }

    // =========================================================================
    // Zona Administración: Cuestionarios
    // =========================================================================

    /**
     * Ej: Administración > Cuestionarios
     */
    public List<BreadcrumbItem> adminCuestionarios() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Cuestionarios", null)
        );
    }

    /**
     * Ej: Administración > Cuestionarios > Nuevo cuestionario
     */
    public List<BreadcrumbItem> adminCuestionarioNuevo() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Cuestionarios", "/admin/cuestionarios"),
            new BreadcrumbItem("Nuevo cuestionario", null)
        );
    }

    /**
     * Ej: Administración > Cuestionarios > Editar '[Título]'
     */
    public List<BreadcrumbItem> adminCuestionarioEditar(String titulo) {
        String label = (titulo != null && !titulo.isBlank()) ? "Editar '" + titulo + "'" : "Editar cuestionario";
        return List.of(
            ADMIN,
            new BreadcrumbItem("Cuestionarios", "/admin/cuestionarios"),
            new BreadcrumbItem(label, null)
        );
    }

    // =========================================================================
    // Zona Administración: Usuarios
    // =========================================================================

    /**
     * Ej: Administración > Usuarios
     */
    public List<BreadcrumbItem> adminUsuarios() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Usuarios", null)
        );
    }

    /**
     * Ej: Administración > Usuarios > Nuevo usuario
     */
    public List<BreadcrumbItem> adminUsuarioNuevo() {
        return List.of(
            ADMIN,
            new BreadcrumbItem("Usuarios", "/admin/usuarios"),
            new BreadcrumbItem("Nuevo usuario", null)
        );
    }

    /**
     * Ej: Administración > Usuarios > Editar '[Nombre/Username]'
     */
    public List<BreadcrumbItem> adminUsuarioEditar(String username) {
        String label = (username != null && !username.isBlank()) ? "Editar '" + username + "'" : "Editar usuario";
        return List.of(
            ADMIN,
            new BreadcrumbItem("Usuarios", "/admin/usuarios"),
            new BreadcrumbItem(label, null)
        );
    }
}
