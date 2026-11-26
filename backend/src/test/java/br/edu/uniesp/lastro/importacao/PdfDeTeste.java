package br.edu.uniesp.lastro.importacao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class PdfDeTeste {

    public static final String CABECALHO_REFERENCIA = "Ref. Interna Produto Unidade Preço KG Preço Fardo";

    private PdfDeTeste() {}

    public static byte[] comLinhas(String... linhas) {
        try (PDDocument documento = new PDDocument();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                conteudo.newLineAtOffset(50, 750);
                for (String linha : linhas) {
                    conteudo.showText(linha);
                    conteudo.newLineAtOffset(0, -14);
                }
                conteudo.endText();
            }
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException excecao) {
            throw new UncheckedIOException(excecao);
        }
    }

    public static byte[] tabelaReferenciaUnica() {
        return comLinhas(CABECALHO_REFERENCIA, "9999 ITEM " + UUID.randomUUID() + " UN 10 10");
    }

    public static byte[] recurso(String nome) {
        try (InputStream entrada = PdfDeTeste.class.getResourceAsStream("/pdfs/" + nome)) {
            return entrada.readAllBytes();
        } catch (IOException excecao) {
            throw new UncheckedIOException(excecao);
        }
    }
}
