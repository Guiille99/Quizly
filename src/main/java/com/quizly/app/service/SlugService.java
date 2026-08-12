package com.quizly.app.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class SlugService {
    private static final Pattern NON_LATIN = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DUPLICATE_HYPHENS = Pattern.compile("-+");

    /**
     * Genera un slug a partir de un texto.
     * 
     * <p>El texto se normaliza eliminando acentos y caracteres especiales,
     * se convierte a minúsculas y los grupos de caracteres no alfanuméricos
     * se sustituyen por un único guión.</p>
     * 
     * @param text
     * @return
     */
    public String generateSlug(String text){
        if (StringUtils.isBlank(text)) {
            return null;
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim();

        String slug = NON_LATIN.matcher(normalized).replaceAll("-");

        slug = DUPLICATE_HYPHENS.matcher(slug).replaceAll("-");

        return slug.replaceAll("^-|-$", "");
    }
}
