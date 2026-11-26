package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConversorPrecoBrasileiroTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "32|32.00",
                "32,5|32.50",
                "32,50|32.50",
                "1.234,56|1234.56",
                "1234,56|1234.56",
                "R$ 35,00|35.00",
                "R$35,00|35.00",
                "0|0.00",
                "1.000.000,00|1000000.00"
            })
    void deveConverterPrecoNoFormatoBrasileiro(String texto, String esperado) {
        assertThat(ConversorPrecoBrasileiro.converter(texto))
                .hasValueSatisfying(preco -> assertThat(preco).isEqualTo(new BigDecimal(esperado)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "3a,5", "32.5", "32,555", "-3", "1,2,3", "12.34", "R$", "1.23,45"})
    void deveRecusarTextoQueNaoEPrecoBrasileiro(String texto) {
        assertThat(ConversorPrecoBrasileiro.converter(texto)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    void deveRecusarPrecoNulo(String texto) {
        assertThat(ConversorPrecoBrasileiro.converter(texto)).isEmpty();
    }
}
