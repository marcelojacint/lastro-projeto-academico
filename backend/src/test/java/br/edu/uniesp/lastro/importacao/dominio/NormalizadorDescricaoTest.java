package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizadorDescricaoTest {

    @Test
    void deveDeixarMaiusculoSemAcentoEComEspacoUnico() {
        assertThat(NormalizadorDescricao.normalizar("  Açúcar   de  coco\t orgânico "))
                .isEqualTo("ACUCAR DE COCO ORGANICO");
    }

    @Test
    void deveManterDescricaoJaNormalizada() {
        assertThat(NormalizadorDescricao.normalizar("DOCANTE STEVIA COLOR ANDINA 40G"))
                .isEqualTo("DOCANTE STEVIA COLOR ANDINA 40G");
    }

    @Test
    void deveDevolverNuloParaTextoVazioOuNulo() {
        assertThat(NormalizadorDescricao.normalizar("   ")).isNull();
        assertThat(NormalizadorDescricao.normalizar(null)).isNull();
    }
}
