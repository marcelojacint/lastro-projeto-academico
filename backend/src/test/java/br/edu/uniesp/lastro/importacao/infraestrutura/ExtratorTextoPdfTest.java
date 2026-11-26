package br.edu.uniesp.lastro.importacao.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.importacao.dominio.FalhaLeituraException;
import br.edu.uniesp.lastro.importacao.dominio.MotivoFalha;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ExtratorTextoPdfTest {

    private final ExtratorTextoPdf extrator = new ExtratorTextoPdf();

    @Test
    void deveExtrairAsLinhasDoPdfDoFornecedor1() {
        assertThat(extrator.extrairLinhas(PdfDeTeste.recurso("Fornecedor_1.pdf")))
                .containsExactly(
                        "Ref. Interna Produto Unidade Preço KG Preço Fardo",
                        "1076 ACUCAR DE COCO KG 32 32",
                        "1077 DOCANTE STEVIA COLOR ANDINA 40G UN 78 78");
    }

    @Test
    void deveExtrairAsLinhasDoPdfDoFornecedor2() {
        assertThat(extrator.extrairLinhas(PdfDeTeste.recurso("Fornecedor_2.pdf")))
                .containsExactly(
                        "Descrição ATACADO VAREJO", "ACUCAR DE COCO 32 32", "DOCANTE STEVIA COLOR ANDINA 40G 78 78");
    }

    @Test
    void deveFalharComPdfIlegivelQuandoOArquivoNaoAbre() {
        byte[] corrompido = "%PDF-1.4\nconteudo quebrado".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> extrator.extrairLinhas(corrompido))
                .isInstanceOfSatisfying(
                        FalhaLeituraException.class,
                        falha -> assertThat(falha.getMotivo()).isEqualTo(MotivoFalha.PDF_ILEGIVEL));
    }

    @Test
    void deveFalharComPdfIlegivelQuandoNaoHaTexto() {
        assertThatThrownBy(() -> extrator.extrairLinhas(PdfDeTeste.comLinhas()))
                .isInstanceOfSatisfying(
                        FalhaLeituraException.class,
                        falha -> assertThat(falha.getMotivo()).isEqualTo(MotivoFalha.PDF_ILEGIVEL));
    }
}
