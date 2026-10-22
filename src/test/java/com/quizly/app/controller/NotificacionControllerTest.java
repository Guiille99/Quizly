package com.quizly.app.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioCompartido;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.CuestionarioCompartidoService;
import com.quizly.app.web.notification.ToastService;

class NotificacionControllerTest {

    private CuestionarioCompartidoService compartidoService;
    private ToastService toastService;
    private NotificacionController controller;
    private QuizlyUserDetails authUser;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        compartidoService = mock(CuestionarioCompartidoService.class);
        toastService = mock(ToastService.class);
        controller = new NotificacionController(compartidoService, toastService);

        usuario = new Usuario();
        usuario.setId(2L);
        usuario.setUsername("receptor");
        authUser = new QuizlyUserDetails(usuario);
    }

    @Test
    @DisplayName("getDropdownContent debe poblar el modelo con pendientes y su conteo")
    void getDropdownContent_populatesModel() {
        CuestionarioCompartido comp = new CuestionarioCompartido();
        comp.setId(10L);
        when(compartidoService.obtenerPendientesParaReceptor(2L)).thenReturn(List.of(comp));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.getDropdownContent(authUser, model);

        assertThat(view).isEqualTo("fragments/notificaciones-dropdown :: dropdown-content");
        assertThat(model.get("compartidosPendientes")).isEqualTo(List.of(comp));
        assertThat(model.get("numNotificacionesPendientes")).isEqualTo(1);
    }

    @Test
    @DisplayName("aceptarCompartido llama al servicio y actualiza el modelo con la lista remanente")
    void aceptarCompartido_exitoso() throws Exception {
        Cuestionario clonado = new Cuestionario();
        clonado.setTitulo("Clon de Prueba");
        when(compartidoService.aceptarCompartido(10L, 2L)).thenReturn(clonado);
        when(compartidoService.obtenerPendientesParaReceptor(2L)).thenReturn(Collections.emptyList());

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = controller.aceptarCompartido(10L, authUser, response, model);

        assertThat(view).isEqualTo("fragments/notificaciones-dropdown :: dropdown-content");
        assertThat(model.get("numNotificacionesPendientes")).isEqualTo(0);
        verify(compartidoService).aceptarCompartido(10L, 2L);
    }

    @Test
    @DisplayName("rechazarCompartido llama al servicio y actualiza el modelo con la lista remanente")
    void rechazarCompartido_exitoso() throws Exception {
        when(compartidoService.obtenerPendientesParaReceptor(2L)).thenReturn(Collections.emptyList());

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = controller.rechazarCompartido(10L, authUser, response, model);

        assertThat(view).isEqualTo("fragments/notificaciones-dropdown :: dropdown-content");
        assertThat(model.get("numNotificacionesPendientes")).isEqualTo(0);
        verify(compartidoService).rechazarCompartido(10L, 2L);
    }
}

