package com.quizly.app.web.notification;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.web.model.Toast;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class ToastService {
    private static final String KEY = "toasts";
    private final ObjectMapper objectMapper;

    public void success(Model model, String msg) {
        // model.addAttribute("toast", Toast.success(msg));
        get(model).add(Toast.success(msg));
    }

    public void success(RedirectAttributes ra, String msg) {
        // ra.addFlashAttribute("toast", Toast.success(msg));
        get(ra).add(Toast.success(msg));
    }

    public void success(HttpServletResponse response, String msg){
        sendTrigger(response, "success", msg);
    }

    public void error(Model model, String msg) {
        // model.addAttribute("toast", Toast.error(msg));
        get(model).add(Toast.error(msg));
    }

    public void error(RedirectAttributes ra, String msg) {
        // ra.addFlashAttribute("toast", Toast.error(msg));
        get(ra).add(Toast.error(msg));
    }

    public void error(HttpServletResponse response, String msg){
        sendTrigger(response, "error", msg);
    }

    public void warning(Model model, String msg) {
        // model.addAttribute("toast", Toast.warning(msg));
        get(model).add(Toast.warning(msg));
    }

    public void warning(RedirectAttributes ra, String msg) {
        // ra.addFlashAttribute("toast", Toast.warning(msg));
        get(ra).add(Toast.warning(msg));
    }

    public void warning(HttpServletResponse response, String msg){
        sendTrigger(response, "warning", msg);
    }

    public void info(Model model, String msg) {
        // model.addAttribute("toast", Toast.info(msg));
        get(model).add(Toast.info(msg));
    }

    public void info(RedirectAttributes ra, String msg) {
        // ra.addFlashAttribute("toast", Toast.info(msg));
        get(ra).add(Toast.info(msg));
    }

    public void info(HttpServletResponse response, String msg){
        sendTrigger(response, "info", msg);
    }
    
    /**
     * Construye y envía un evento personalizado al frontend utilizando la cabecera
     * {@code HT-Trigger} de HTMX.
     * 
     * <p>
     * Este método serializa los parámetros proporcionados en una estructura JSON compatible con el 
     * listener del cliente (ej. {@code {"showToast": {"tipo": "success", "mensaje": "..."}}}). 
     * Esto permite disparar notificaciones asíncronas desde el servidor sin necesidad de alterar 
     * los fragmentos HTML devueltos.
     * </p>
     * 
     * @param response
     * @param tipo
     * @param mensaje
     */
    private void sendTrigger(HttpServletResponse response, String tipo, String mensaje){
        try {
            Map<String, Object> payload = Map.of(
                "tipo", tipo,
                "mensaje", mensaje
            );

            Map<String, Object> triggerMap = Map.of("showToast", payload);

            String jsonHeader = objectMapper.writeValueAsString(triggerMap);
            response.setHeader("HX-Trigger", jsonHeader);
        } catch (Exception e) {
            log.error("Se ha producido un error al serializar el toast de respuesta", e);
        }
    }

    // =========================
    // Helpers
    // =========================

    @SuppressWarnings("unchecked")
    private List<Toast> get(Model model) {
        List<Toast> toasts = (List<Toast>) model.getAttribute(KEY);
        if (toasts == null) {
            toasts = new ArrayList<>();
            model.addAttribute(KEY, toasts);
        }
        return toasts;
    }
    
    @SuppressWarnings("unchecked")
    private List<Toast> get(RedirectAttributes ra) {
        List<Toast> toasts = (List<Toast>) ra.getFlashAttributes().get(KEY);
        if (toasts == null) {
            toasts = new ArrayList<>();
            ra.addFlashAttribute(KEY, toasts);
        }
        return toasts;
    }

}
