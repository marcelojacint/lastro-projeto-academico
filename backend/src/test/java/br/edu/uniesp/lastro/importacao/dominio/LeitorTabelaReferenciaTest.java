package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeitorTabelaReferenciaTest {

    private static final String CABECALHO = "Ref. Interna Produto Unidade Preço KG Preço Fardo";

    private final LeitorTabelaReferencia leitor = new LeitorTabelaReferencia();

    @Test
    void deveGerarDuasApresentacoesPorLinhaDoPdfDeReferencia() {
        List<ItemExtraido> itens = leitor.ler(
                List.of(CABECALHO, "1076 ACUCAR DE COCO KG 32 32", "1077 DOCANTE STEVIA COLOR ANDINA 40G UN 78 78"));

        assertThat(itens)
                .extracting(
                        ItemExtraido::numeroLinha,
                        ItemExtraido::referencia,
                        ItemExtraido::descricao,
                        ItemExtraido::unidade,
                        ItemExtraido::modalidadePreco,
                        ItemExtraido::preco)
                .containsExactly(
                        tuple(2, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.UNITARIO, new BigDecimal("32.00")),
                        tuple(2, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.FARDO, new BigDecimal("32.00")),
                        tuple(
                                3,
                                "1077",
                                "DOCANTE STEVIA COLOR ANDINA 40G",
                                "UN",
                                ModalidadePreco.UNITARIO,
                                new BigDecimal("78.00")),
                        tuple(
                                3,
                                "1077",
                                "DOCANTE STEVIA COLOR ANDINA 40G",
                                "UN",
                                ModalidadePreco.FARDO,
                                new BigDecimal("78.00")));
        assertThat(itens).allSatisfy(item -> {
            assertThat(item.quantidade()).isNull();
            assertThat(item.errosDeLeitura()).isEmpty();
        });
    }

    @Test
    void deveLerPrecoComReaisEMilhar() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "2001 CHA DE HIBISCO 100G UN R$ 35,00 R$ 1.234,56"));

        assertThat(itens)
                .extracting(ItemExtraido::preco)
                .containsExactly(new BigDecimal("35.00"), new BigDecimal("1234.56"));
        assertThat(itens).extracting(ItemExtraido::descricao).containsOnly("CHA DE HIBISCO 100G");
    }

    @Test
    void deveGuardarLinhaOriginalETratarLinhaForaDoPadraoComoUmItemComErro() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "Pedido mínimo de R$ 300,00"));

        assertThat(itens).singleElement().satisfies(item -> {
            assertThat(item.numeroLinha()).isEqualTo(2);
            assertThat(item.linhaOriginal()).isEqualTo("Pedido mínimo de R$ 300,00");
            assertThat(item.modalidadePreco()).isNull();
            assertThat(item.errosDeLeitura())
                    .containsExactly(new MensagemErro("LINHA_FORA_DO_PADRAO", "Linha fora do padrão do layout"));
        });
    }

    @Test
    void deveMarcarComoErroDoItemSoOPrecoQueNaoConverte() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "1076 ACUCAR DE COCO KG 3a,5 32"));

        assertThat(itens.get(0).preco()).isNull();
        assertThat(itens.get(0).errosDeLeitura())
                .containsExactly(new MensagemErro("PRECO_ILEGIVEL", "Preço \"3a,5\" não é um valor válido"));
        assertThat(itens.get(1).preco()).isEqualByComparingTo("32");
        assertThat(itens.get(1).errosDeLeitura()).isEmpty();
    }

    @Test
    void deveCortarLinhaMaiorQueOLimiteDaColuna() {
        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, "1080 " + "PRODUTO ".repeat(150) + "UN 10 10"));

        assertThat(itens).singleElement().satisfies(item -> {
            assertThat(item.linhaOriginal()).hasSize(1000);
            assertThat(item.errosDeLeitura()).extracting(MensagemErro::codigo).containsExactly("LINHA_FORA_DO_PADRAO");
        });
    }

    @Test
    void deveTratarDescricaoMaiorQueOLimiteComoLinhaForaDoPadrao() {
        String linha = "1081 " + "PRODUTO ".repeat(45) + "UN 10 10";

        List<ItemExtraido> itens = leitor.ler(List.of(CABECALHO, linha));

        assertThat(itens).singleElement().satisfies(item -> {
            assertThat(item.linhaOriginal()).isEqualTo(linha);
            assertThat(item.errosDeLeitura()).extracting(MensagemErro::codigo).containsExactly("LINHA_FORA_DO_PADRAO");
        });
    }

    @Test
    void deveFalharQuandoNaoEncontraOCabecalhoDoLayout() {
        assertThatThrownBy(() -> leitor.ler(List.of("Descrição ATACADO VAREJO", "ACUCAR DE COCO 32 32")))
                .isInstanceOfSatisfying(
                        FalhaLeituraException.class,
                        falha -> assertThat(falha.getMotivo()).isEqualTo(MotivoFalha.LAYOUT_NAO_RECONHECIDO));
    }
}
