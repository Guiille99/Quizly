package com.quizly.app.util;

import java.time.Duration;
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

    /**
     * Formatea la duración en segundos usando la unidad más adecuada.
     * <br>
     * Ejemplos:
     * <ul>
     * <li> 45s    -> 0 min </li>
     * <li> 1800s  -> 30 min </li>
     * <li> 5400s  -> 1 h 30 min </li>
     * <li> 9000s  -> 6 d 6 h </li>
     * </ul>
     * 
     * @param totalSeconds
     * @return
     */
    public static String formatDuration(long totalSeconds){
        Duration duration = Duration.ofSeconds(totalSeconds);
        long days = duration.toDays();

        if (days > 0) {
            return String.format("%d d %d h", days, duration.toHoursPart());
        }

        long hours = duration.toHours();

        if (hours > 0) {
            return String.format("%d h %d min", hours, duration.toMinutesPart());
        }

        return duration.toMinutes() + " min";
    }

    /**
     * Formatea una duración en segundos a un formato natural en español.
     * Ejemplos: "1 hora y 20 minutos", "10 minutos", "1 minuto", "2 horas"
     * 
     * @param totalSeconds segundos a formatear
     * @return cadena formateada
     */
    public static String formatNaturalDuration(long totalSeconds) {
        if (totalSeconds == 0) return "0 minutos";
        
        Duration duration = Duration.ofSeconds(totalSeconds);
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hora" : " horas");
        }

        if (minutes > 0) {
            if (sb.length() > 0) {
                sb.append(" y ");
            }
            sb.append(minutes).append(minutes == 1 ? " minuto" : " minutos");
        }
        
        // En caso de que haya segundos pero no lleguen a un minuto completo
        if (sb.length() == 0 && totalSeconds > 0) {
            return "menos de un minuto";
        }

        return sb.toString();
    }
}
