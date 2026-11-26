package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.util.Map;
import java.util.Optional;

public final class LeitoresPdf {

    private static final Map<LayoutPdf, LeitorPdf> POR_LAYOUT =
            Map.of(LayoutPdf.TABELA_REFERENCIA, new LeitorTabelaReferencia());

    private LeitoresPdf() {}

    public static Optional<LeitorPdf> para(LayoutPdf layout) {
        return Optional.ofNullable(POR_LAYOUT.get(layout));
    }
}
