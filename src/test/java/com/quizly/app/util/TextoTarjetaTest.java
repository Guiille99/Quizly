package com.quizly.app.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextoTarjetaTest {

    @Test
    void recortaYColapsaEspacios() {
        assertEquals("a b c", TextoTarjeta.normalizar("  a \n b\t c  ", 50));
    }

    @Test
    void truncaConElipsisSinSuperarElMaximo() {
        String resultado = TextoTarjeta.normalizar("x".repeat(600), TextoTarjeta.MAX_ANVERSO);

        assertEquals(TextoTarjeta.MAX_ANVERSO, resultado.length());
        assertEquals("…", resultado.substring(resultado.length() - 1));
    }

    @Test
    void nuloDaCadenaVacia() {
        assertEquals("", TextoTarjeta.normalizar(null, 10));
    }

    @Test
    void componerReversoUneCorrectasYAnadeRetroalimentacion() {
        assertEquals("A; B — Porque sí", TextoTarjeta.componerReverso(List.of("A", "B"), "Porque sí"));
        assertEquals("A", TextoTarjeta.componerReverso(List.of("A"), "  "));
        assertEquals("A", TextoTarjeta.componerReverso(List.of("A"), null));
    }

    @Test
    void componerReversoNoSuperaElMaximo() {
        assertEquals(TextoTarjeta.MAX_REVERSO,
                TextoTarjeta.componerReverso(List.of("x".repeat(900)), "y".repeat(300)).length());
    }

    @Test
    void claveIgnoraMayusculasYEspacios() {
        assertEquals(TextoTarjeta.clave("¿Qué es   Java?"), TextoTarjeta.clave(" ¿qué es java? "));
    }
}
