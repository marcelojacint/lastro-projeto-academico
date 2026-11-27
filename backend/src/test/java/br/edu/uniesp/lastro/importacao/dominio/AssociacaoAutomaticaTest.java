package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.uniesp.lastro.importacao.dominio.CatalogoProdutos.ProdutoDoCatalogo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AssociacaoAutomaticaTest {

    private final CatalogoProdutos catalogo = CatalogoProdutos.dosProdutosAtivos(List.of(
            new ProdutoDoCatalogo(10L, "Açúcar de Coco"),
            new ProdutoDoCatalogo(20L, "Docante Stevia Color Andina 40g"),
            new ProdutoDoCatalogo(30L, "Granola"),
            new ProdutoDoCatalogo(31L, "GRANOLA")));

    @Test
    void deveUsarOVinculoAtivoDaApresentacaoQuandoExiste() {
        Optional<DecisaoAssociacao> decisao = DecisaoAssociacao.decidir(Optional.of(99L), "ACUCAR DE COCO", catalogo);

        assertThat(decisao).contains(new DecisaoAssociacao(99L, false));
    }

    @Test
    void deveAssociarPorCorrespondenciaExataIgnorandoAcentoEMaiusculas() {
        Optional<DecisaoAssociacao> decisao = DecisaoAssociacao.decidir(Optional.empty(), "ACUCAR DE COCO", catalogo);

        assertThat(decisao).contains(new DecisaoAssociacao(10L, true));
    }

    @Test
    void deveIgnorarEspacosRepetidosNaDescricaoDoPdf() {
        assertThat(catalogo.produtoComNomeExato("  DOCANTE   STEVIA COLOR ANDINA 40G "))
                .contains(20L);
    }

    @Test
    void deveNaoAssociarDescricaoParecida() {
        assertThat(DecisaoAssociacao.decidir(Optional.empty(), "ACUCAR DE COCO ORGANICO", catalogo))
                .isEmpty();
        assertThat(DecisaoAssociacao.decidir(Optional.empty(), "ACUCAR COCO", catalogo))
                .isEmpty();
    }

    @Test
    void deveNaoAssociarQuandoDoisProdutosTemOMesmoNome() {
        assertThat(DecisaoAssociacao.decidir(Optional.empty(), "granola", catalogo))
                .isEmpty();
    }

    @Test
    void deveNaoAssociarItemSemDescricao() {
        assertThat(DecisaoAssociacao.decidir(Optional.empty(), null, catalogo)).isEmpty();
    }

    @Test
    void deveDeixarValidoSoItemSemErroEAssociado() {
        ResultadoValidacaoItem semErro = new ResultadoValidacaoItem(SituacaoItem.PENDENTE_ASSOCIACAO, List.of());
        ResultadoValidacaoItem comErro =
                new ResultadoValidacaoItem(SituacaoItem.INVALIDO, List.of(ErroItem.PRECO_ZERO.paraMensagem()));

        assertThat(semErro.situacaoComAssociacao(true)).isEqualTo(SituacaoItem.VALIDO);
        assertThat(semErro.situacaoComAssociacao(false)).isEqualTo(SituacaoItem.PENDENTE_ASSOCIACAO);
        assertThat(comErro.situacaoComAssociacao(true)).isEqualTo(SituacaoItem.INVALIDO);
    }
}
