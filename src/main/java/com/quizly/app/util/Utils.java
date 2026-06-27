package com.quizly.app.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class Utils {
    public String formatFechaRelativa(LocalDateTime fecha) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(fecha, now);
        
        if (minutes < 1) return "ahora mismo";
        if (minutes < 60) return "hace " + minutes + "m";
        
        long hours = ChronoUnit.HOURS.between(fecha, now);
        if (hours < 24) return "hace " + hours + "h";
        
        if (ChronoUnit.DAYS.between(fecha.toLocalDate(), now.toLocalDate()) == 1) {
            return "ayer";
        }
        
        if (ChronoUnit.DAYS.between(fecha, now) < 7) {
            String dia = fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            return dia.endsWith(".") ? dia : dia + ".";
        }
        
        return fecha.format(DateTimeFormatter.ofPattern("dd MMM", new Locale("es", "ES")));
    }

    public static String formatFecha(LocalDateTime fecha, String patron){
        if (fecha == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patron);
        return fecha.format(formatter);
    }
}
