package br.edu.uniesp.lastro.importacao.dominio;

import java.text.Normalizer;
import java.util.Locale;

public final class NormalizadorDescricao {

    private NormalizadorDescricao() {}

    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String normalizado =
                semAcento.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
