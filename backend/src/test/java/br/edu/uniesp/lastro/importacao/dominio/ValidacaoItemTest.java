package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidacaoItemTest {

    @Test
    void deveDeixarItemSemErrosPendenteDeAssociacao() {
        ResultadoValidacaoItem resultado =
                ValidacaoItem.validar(item("1076", "ACUCAR DE COCO", "KG", "32"), LayoutPdf.TABELA_REFERENCIA);

        assertThat(resultado.situacao()).isEqualTo(SituacaoItem.PENDENTE_ASSOCIACAO);
        assertThat(resultado.erros()).isEmpty();
        assertThat(resultado.invalido()).isFalse();
    }

    @Test
    void deveDeixarInvalidoQuandoPrecoAusente() {
        ResultadoValidacaoItem resultado =
                ValidacaoItem.validar(item("1076", "ACUCAR DE COCO", "KG", null), LayoutPdf.TABELA_REFERENCIA);

        assertThat(resultado.situacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(resultado.erros()).containsExactly(new MensagemErro("PRECO_AUSENTE", "Preço não informado"));
    }

    @Test
    void deveDeixarInvalidoQuandoPrecoZero() {
        ResultadoValidacaoItem resultado = ValidacaoItem.validar(
                item("1077", "DOCANTE STEVIA COLOR ANDINA 40G", "UN", "0.00"), LayoutPdf.TABELA_REFERENCIA);

        assertThat(resultado.situacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(resultado.erros()).containsExactly(new MensagemErro("PRECO_ZERO", "Preço igual a zero"));
    }

    @Test
    void deveDeixarInvalidoQuandoPrecoNegativo() {
        ResultadoValidacaoItem resultado =
                ValidacaoItem.validar(item(null, "ACUCAR DE COCO", null, "-3"), LayoutPdf.ATACADO_VAREJO);

        assertThat(resultado.situacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(resultado.erros()).containsExactly(new MensagemErro("PRECO_NEGATIVO", "Preço negativo"));
    }

    @Test
    void deveJuntarTodosOsErrosDoItemNaOrdemDosCampos() {
        ItemExtraido semNada = new ItemExtraido(7, "??? 0", null, "  ", null, null, null, BigDecimal.ZERO, List.of());

        ResultadoValidacaoItem resultado = ValidacaoItem.validar(semNada, LayoutPdf.TABELA_REFERENCIA);

        assertThat(resultado.situacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(resultado.erros())
                .extracting(MensagemErro::codigo)
                .containsExactly(
                        "REFERENCIA_AUSENTE", "DESCRICAO_VAZIA", "UNIDADE_AUSENTE", "MODALIDADE_AUSENTE", "PRECO_ZERO");
        assertThat(resultado.erros())
                .allSatisfy(erro -> assertThat(erro.mensagem()).isNotBlank());
    }

    @Test
    void deveExigirReferenciaEUnidadeSoNoLayoutComReferencia() {
        ItemExtraido semReferenciaNemUnidade = item(null, "ACUCAR DE COCO", null, "32");

        assertThat(ValidacaoItem.validar(semReferenciaNemUnidade, LayoutPdf.ATACADO_VAREJO)
                        .erros())
                .isEmpty();
        assertThat(ValidacaoItem.validar(semReferenciaNemUnidade, LayoutPdf.TABELA_REFERENCIA)
                        .erros())
                .extracting(MensagemErro::codigo)
                .containsExactly("REFERENCIA_AUSENTE", "UNIDADE_AUSENTE");
    }

    @Test
    void deveManterErroDeLeituraDoPrecoSemRepetirPrecoAusente() {
        MensagemErro ilegivel = new MensagemErro("PRECO_ILEGIVEL", "Preço \"3a,5\" não é um valor válido");
        ItemExtraido lido = new ItemExtraido(
                4,
                "1076 ACUCAR DE COCO KG 3a,5",
                "1076",
                "ACUCAR DE COCO",
                "KG",
                ModalidadePreco.UNITARIO,
                null,
                null,
                List.of(ilegivel));

        ResultadoValidacaoItem resultado = ValidacaoItem.validar(lido, LayoutPdf.TABELA_REFERENCIA);

        assertThat(resultado.situacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(resultado.erros()).containsExactly(ilegivel);
    }

    @Test
    void deveJuntarErroDeLeituraComErrosDosCampos() {
        MensagemErro linhaForaDoPadrao = new MensagemErro("LINHA_FORA_DO_PADRAO", "Linha fora do padrão do layout");
        ItemExtraido lido =
                new ItemExtraido(9, "texto solto", null, null, null, null, null, null, List.of(linhaForaDoPadrao));

        ResultadoValidacaoItem resultado = ValidacaoItem.validar(lido, LayoutPdf.ATACADO_VAREJO);

        assertThat(resultado.erros())
                .extracting(MensagemErro::codigo)
                .containsExactly("LINHA_FORA_DO_PADRAO", "DESCRICAO_VAZIA", "MODALIDADE_AUSENTE", "PRECO_AUSENTE");
    }

    private static ItemExtraido item(String referencia, String descricao, String unidade, String preco) {
        return new ItemExtraido(
                1,
                "linha do pdf",
                referencia,
                descricao,
                unidade,
                ModalidadePreco.UNITARIO,
                null,
                preco == null ? null : new BigDecimal(preco),
                List.of());
    }
}
