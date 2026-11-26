package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.FalhaLeituraException;
import br.edu.uniesp.lastro.importacao.dominio.MotivoFalha;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class ExtratorTextoPdf {

    public List<String> extrairLinhas(byte[] conteudo) {
        try (PDDocument documento = Loader.loadPDF(conteudo)) {
            PDFTextStripper extrator = new PDFTextStripper();
            extrator.setSortByPosition(true);
            List<String> linhas = extrator.getText(documento)
                    .lines()
                    .map(String::strip)
                    .filter(linha -> !linha.isEmpty())
                    .toList();
            if (linhas.isEmpty()) {
                throw new FalhaLeituraException(MotivoFalha.PDF_ILEGIVEL, "O PDF não tem texto legível");
            }
            return linhas;
        } catch (IOException excecao) {
            throw new FalhaLeituraException(MotivoFalha.PDF_ILEGIVEL, "Não foi possível abrir o PDF");
        }
    }
}
