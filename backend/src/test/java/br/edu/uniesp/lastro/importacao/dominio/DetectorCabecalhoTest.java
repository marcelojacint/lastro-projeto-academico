package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import br.edu.uniesp.lastro.importacao.dominio.DetectorCabecalho.CabecalhoDetectado;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectorCabecalhoTest {

    @Test
    void deveReconhecerCabecalhoDaTabelaComReferencia() {
        List<String> linhas = List.of(
                "Tabela de preços novembro",
                "Ref. Interna Produto Unidade Preço KG Preço Fardo",
                "1076 ACUCAR DE COCO KG 32 32");

        assertThat(DetectorCabecalho.detectar(linhas)).contains(new CabecalhoDetectado(LayoutPdf.TABELA_REFERENCIA, 1));
    }

    @Test
    void deveReconhecerCabecalhoDeAtacadoEVarejo() {
        assertThat(DetectorCabecalho.detectar(List.of("Descrição ATACADO VAREJO", "ACUCAR DE COCO 32 32")))
                .contains(new CabecalhoDetectado(LayoutPdf.ATACADO_VAREJO, 0));
    }

    @Test
    void deveNaoReconhecerTextoSemCabecalho() {
        assertThat(DetectorCabecalho.detectar(List.of("Relatório mensal", "1076 ACUCAR DE COCO KG 32 32")))
                .isEmpty();
    }
}
