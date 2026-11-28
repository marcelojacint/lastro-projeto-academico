package br.edu.uniesp.lastro.precificacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClassificadorPrecoTest {

    @Test
    void deveClassificarComoAumentouQuandoPrecoSobe() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("33.00"), preco("35.00"), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.AUMENTOU);
        assertThat(resultado.precoAnterior()).isEqualTo(preco("33.00"));
        assertThat(resultado.precoAtual()).isEqualTo(preco("35.00"));
        assertThat(resultado.diferenca()).isEqualTo(preco("2.00"));
        assertThat(resultado.variacaoPercentual()).isEqualTo(preco("6.06"));
    }

    @Test
    void deveClassificarComoDiminuiuQuandoPrecoCai() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("15.00"), preco("14.00"), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.DIMINUIU);
        assertThat(resultado.diferenca()).isEqualTo(preco("-1.00"));
        assertThat(resultado.variacaoPercentual()).isEqualTo(preco("-6.67"));
    }

    @Test
    void deveClassificarSemAlteracaoQuandoPrecoIgual() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("38.00"), preco("38.00"), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.SEM_ALTERACAO);
        assertThat(resultado.diferenca()).isEqualTo(preco("0.00"));
        assertThat(resultado.variacaoPercentual()).isEqualTo(preco("0.00"));
    }

    @Test
    void deveClassificarComoNovoSemPrecoAnterior() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(null, preco("20.00"), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.NOVO);
        assertThat(resultado.precoAnterior()).isNull();
        assertThat(resultado.precoAtual()).isEqualTo(preco("20.00"));
        assertThat(resultado.diferenca()).isNull();
        assertThat(resultado.variacaoPercentual()).isNull();
    }

    @Test
    void deveDeixarPendenteQuandoPrecoZero() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("32.00"), preco("0.00"), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.PENDENTE);
        assertThat(resultado.precoAnterior()).isEqualTo(preco("32.00"));
        assertThat(resultado.diferenca()).isNull();
        assertThat(resultado.variacaoPercentual()).isNull();
    }

    @Test
    void deveDeixarPendenteQuandoItemSemVinculo() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(null, preco("20.00"), false);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.PENDENTE);
        assertThat(resultado.precoAtual()).isEqualTo(preco("20.00"));
        assertThat(resultado.diferenca()).isNull();
        assertThat(resultado.variacaoPercentual()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-5.00"})
    void deveDeixarPendenteQuandoPrecoAusenteOuNegativo(String atual) {
        ResultadoComparacao resultado =
                ClassificadorPreco.classificar(preco("32.00"), atual == null ? null : preco(atual), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.PENDENTE);
    }

    @Test
    void deveDeixarPendenteQuandoItemSemVinculoMesmoComPrecoAnterior() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("15.00"), preco("14.00"), false);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.PENDENTE);
        assertThat(resultado.diferenca()).isNull();
    }

    @Test
    void deveArredondarPercentualParaCimaNaMetade() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("8.00"), preco("8.01"), true);

        assertThat(resultado.variacaoPercentual()).isEqualTo(preco("0.13"));
    }

    @Test
    void deveMultiplicarPorCemAntesDeDividir() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("3.00"), preco("4.00"), true);

        assertThat(resultado.variacaoPercentual()).isEqualTo(preco("33.33"));
    }

    @Test
    void deveDeixarPercentualNuloQuandoPrecoAnteriorZero() {
        ResultadoComparacao resultado = ClassificadorPreco.classificar(preco("0.00"), preco("10.00"), true);

        assertThat(resultado.classificacao()).isEqualTo(Classificacao.AUMENTOU);
        assertThat(resultado.diferenca()).isEqualTo(preco("10.00"));
        assertThat(resultado.variacaoPercentual()).isNull();
    }

    private static BigDecimal preco(String valor) {
        return new BigDecimal(valor);
    }
}
