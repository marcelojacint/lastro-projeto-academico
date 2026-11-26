package br.edu.uniesp.lastro.importacao.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConversorPrecoBrasileiro {

    private static final int CASAS_DECIMAIS = 2;
    private static final Pattern FORMATO =
            Pattern.compile("^(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d{1,2}))?$");

    private ConversorPrecoBrasileiro() {}

    public static Optional<BigDecimal> converter(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        Matcher partes = FORMATO.matcher(texto.strip());
        if (!partes.matches()) {
            return Optional.empty();
        }
        String inteiro = partes.group(1).replace(".", "");
        String decimais = partes.group(2) == null ? "0" : partes.group(2);
        return Optional.of(new BigDecimal(inteiro + "." + decimais).setScale(CASAS_DECIMAIS, RoundingMode.UNNECESSARY));
    }
}
