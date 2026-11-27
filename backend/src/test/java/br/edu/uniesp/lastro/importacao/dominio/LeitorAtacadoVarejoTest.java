package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeitorAtacadoVarejoTest {

    private static final String CABECALHO = "Descrição ATACADO VAREJO";

    private final LeitorAtacadoVarejo leitor = new LeitorAtacadoVarejo();

    @Test
    void deveGerarAtacadoEVarejoParaCadaLinhaDoPdfDeReferencia() {
        List<ItemExtraido> itens =
                leitor.ler(List.of(CABECALHO, "ACUCAR DE COCO 32 32", "DOCANTE STEVIA COLOR ANDINA 40G 78 78"));

        assertThat(itens)
                .extracting(
                        ItemExtraido::numeroLinha,
                        ItemExtraido::descricao,
                        ItemExtraido::modalidadePreco,
                        ItemExtraido::preco)
                .containsExactly(
                        tuple(2, "ACUCAR DE COCO", ModalidadePreco.ATACADO, new BigDecimal("32.00")),
                        tuple(2, "ACUCAR DE COCO", ModalidadePreco.VAREJO, new BigDecimal("32.00")),
                        tuple(3, "DOCANTE STEVIA COLOR ANDINA 40G", ModalidadePreco.ATACADO, new BigDecimal("78.00")),
                        tuple(3, "DOCANTE STEVIA COLOR ANDINA 40G", ModalidadePreco.VAREJO, new BigDecimal("78.00")));
        assertThat(itens).allSatisfy(item -> {
            assertThat(item.referencia()).isNull();
            assertThat(item.unidade()).isNull();
            assertThat(item.errosDeLeitura()).isEmpty();
        });
    }

    @Test
    void deveSerValidoNaValidacaoDoLayoutSemReferenciaNemUnidade() {
        ItemExtraido item =
                leitor.ler(List.of(CABECALHO, "ACUCAR DE COCO 32 32")).getFirst();

        assertThat(ValidacaoItem.validar(item, LayoutPdf.ATACADO_VAREJO).erros())
                .isEmpty();
    }

    @Test
    void deveLerPrecosComReaisEMilhar() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "CASTANHA DE CAJU 1KG R$ 1.234,56 R$1.350,00"));

        assertThat(itens)
                .extracting(ItemExtraido::preco)
                .containsExactly(new BigDecimal("1234.56"), new BigDecimal("1350.00"));
        assertThat(itens).extracting(ItemExtraido::descricao).containsOnly("CASTANHA DE CAJU 1KG");
    }

    @Test
    void deveTratarLinhaSemDoisValoresNoFinalComoForaDoPadrao() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "Pedido mínimo de R$ 300,00", "Frete grátis"));

        assertThat(itens)
                .extracting(ItemExtraido::linhaOriginal)
                .containsExactly("Pedido mínimo de R$ 300,00", "Frete grátis");
        assertThat(itens)
                .flatExtracting(ItemExtraido::errosDeLeitura)
                .extracting(MensagemErro::codigo)
                .containsOnly("LINHA_FORA_DO_PADRAO");
    }

    @Test
    void deveMarcarComoErroDoItemSoOPrecoQueNaoConverte() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "ACUCAR DE COCO 3a,5 32"));

        assertThat(itens.get(0).errosDeLeitura())
                .containsExactly(new MensagemErro("PRECO_ILEGIVEL", "Preço \"3a,5\" não é um valor válido"));
        assertThat(itens.get(1).preco()).isEqualByComparingTo("32");
    }

    @Test
    void deveTratarDescricaoMaiorQueOLimiteComoLinhaForaDoPadrao() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "PRODUTO ".repeat(45) + "10 9"));

        assertThat(itens)
                .singleElement()
                .satisfies(item -> assertThat(item.errosDeLeitura())
                        .extracting(MensagemErro::codigo)
                        .containsExactly("LINHA_FORA_DO_PADRAO"));
    }

    @Test
    void deveFalharQuandoOPdfEDoLayoutComReferencia() {
        assertThatThrownBy(() -> leitor.ler(
                        List.of("Ref. Interna Produto Unidade Preço KG Preço Fardo", "1076 ACUCAR DE COCO KG 32 32")))
                .isInstanceOfSatisfying(
                        FalhaLeituraException.class,
                        falha -> assertThat(falha.getMotivo()).isEqualTo(MotivoFalha.LAYOUT_NAO_RECONHECIDO));
    }

    @Test
    void deveEscolherOLeitorPeloLayoutDoFornecedor() {
        assertThat(LeitoresPdf.para(LayoutPdf.ATACADO_VAREJO)).isInstanceOf(LeitorAtacadoVarejo.class);
        assertThat(LeitoresPdf.para(LayoutPdf.TABELA_REFERENCIA)).isInstanceOf(LeitorTabelaReferencia.class);
    }
}
