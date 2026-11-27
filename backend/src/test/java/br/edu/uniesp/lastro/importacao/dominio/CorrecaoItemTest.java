package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorrecaoItemTest {

    private static final UUID IMPORTACAO = UUID.fromString("4f1c2a8e-0b7d-4c55-9a52-3f6b9d1e2a10");

    private final ItemImportado precoIlegivel = ItemImportado.registrar(
            IMPORTACAO,
            new ItemExtraido(
                    4,
                    "1076 ACUCAR DE COCO KG 3a,5 32",
                    "1076",
                    "ACUCAR DE COCO",
                    "KG",
                    ModalidadePreco.UNITARIO,
                    null,
                    null,
                    List.of(ErroItem.PRECO_ILEGIVEL.paraMensagem("3a,5"))),
            SituacaoItem.INVALIDO,
            List.of(ErroItem.PRECO_ILEGIVEL.paraMensagem("3a,5")),
            7L);

    @Test
    void deveManterLinhaModalidadeEQuantidadeEDescartarErrosDeLeitura() {
        ItemExtraido corrigido =
                new CorrecaoItem("1076", "  Acucar   de coco ", "kg", new BigDecimal("35.5")).sobre(precoIlegivel);

        assertThat(corrigido.numeroLinha()).isEqualTo(4);
        assertThat(corrigido.linhaOriginal()).isEqualTo("1076 ACUCAR DE COCO KG 3a,5 32");
        assertThat(corrigido.modalidadePreco()).isEqualTo(ModalidadePreco.UNITARIO);
        assertThat(corrigido.descricao()).isEqualTo("Acucar de coco");
        assertThat(corrigido.unidade()).isEqualTo("KG");
        assertThat(corrigido.preco()).isEqualByComparingTo("35.5");
        assertThat(corrigido.errosDeLeitura()).isEmpty();
    }

    @Test
    void deveRevalidarOItemCorrigido() {
        precoIlegivel.corrigir(
                new CorrecaoItem("1076", "ACUCAR DE COCO", "KG", new BigDecimal("35")).sobre(precoIlegivel),
                LayoutPdf.TABELA_REFERENCIA,
                7L,
                true);

        assertThat(precoIlegivel.getSituacao()).isEqualTo(SituacaoItem.VALIDO);
        assertThat(precoIlegivel.getErros()).isEmpty();
        assertThat(precoIlegivel.getPreco()).isEqualByComparingTo("35");

        precoIlegivel.corrigir(
                new CorrecaoItem("1076", " ", "KG", BigDecimal.ZERO).sobre(precoIlegivel),
                LayoutPdf.TABELA_REFERENCIA,
                null,
                false);

        assertThat(precoIlegivel.getSituacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(precoIlegivel.getErros())
                .extracting(MensagemErro::codigo)
                .containsExactly("DESCRICAO_VAZIA", "PRECO_ZERO");
        assertThat(precoIlegivel.getApresentacaoId()).isNull();
    }

    @Test
    void deveRecusarCamposMaioresQueAsColunasEPrecoComMaisDeDuasCasas() {
        CorrecaoItem correcao =
                new CorrecaoItem("9".repeat(51), "X".repeat(301), "QUILOGRAMAS", new BigDecimal("1.234"));

        assertThatThrownBy(() -> correcao.sobre(precoIlegivel))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("referencia", "descricao", "unidade", "preco"));
    }

    @Test
    void deveAceitarCorrecaoSoComImportacaoEmConferencia() {
        Importacao importacao = Importacao.receber(1L, "tabela.pdf", "a".repeat(64), 100, Instant.now());

        assertThatThrownBy(importacao::exigirEmConferencia).isInstanceOf(ConflitoException.class);

        importacao.iniciarConferencia(4, 1);
        importacao.atualizarContadores(4, 3);

        assertThat(importacao.getItensValidos()).isEqualTo(3);
        assertThat(importacao.getItensPendentes()).isEqualTo(1);

        importacao.falhar(MotivoFalha.PDF_ILEGIVEL);
        assertThatThrownBy(() -> importacao.atualizarContadores(4, 4)).isInstanceOf(ConflitoException.class);
    }
}
