package br.edu.uniesp.lastro.precificacao.dominio;

import java.math.BigDecimal;

public record ResultadoComparacao(
        BigDecimal precoAnterior,
        BigDecimal precoAtual,
        BigDecimal diferenca,
        BigDecimal variacaoPercentual,
        Classificacao classificacao) {}
