package br.edu.uniesp.lastro.precificacao.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ClassificadorPreco {

    private static final int CASAS_DECIMAIS = 2;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private ClassificadorPreco() {}

    public static ResultadoComparacao classificar(BigDecimal precoAnterior, BigDecimal precoAtual, boolean itemValido) {
        if (!itemValido || precoAtual == null || precoAtual.signum() <= 0) {
            return new ResultadoComparacao(precoAnterior, precoAtual, null, null, Classificacao.PENDENTE);
        }
        if (precoAnterior == null) {
            return new ResultadoComparacao(null, precoAtual, null, null, Classificacao.NOVO);
        }
        BigDecimal diferenca = precoAtual.subtract(precoAnterior).setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
        return new ResultadoComparacao(
                precoAnterior,
                precoAtual,
                diferenca,
                calcularVariacaoPercentual(precoAnterior, diferenca),
                classificarPelaDiferenca(diferenca));
    }

    private static BigDecimal calcularVariacaoPercentual(BigDecimal precoAnterior, BigDecimal diferenca) {
        if (precoAnterior.signum() <= 0) {
            return null;
        }
        return diferenca.multiply(CEM).divide(precoAnterior, CASAS_DECIMAIS, RoundingMode.HALF_UP);
    }

    private static Classificacao classificarPelaDiferenca(BigDecimal diferenca) {
        return switch (diferenca.signum()) {
            case 1 -> Classificacao.AUMENTOU;
            case -1 -> Classificacao.DIMINUIU;
            default -> Classificacao.SEM_ALTERACAO;
        };
    }
}
