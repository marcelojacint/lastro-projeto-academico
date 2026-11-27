package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;

public final class LeitoresPdf {

    private static final LeitorPdf TABELA_REFERENCIA = new LeitorTabelaReferencia();
    private static final LeitorPdf ATACADO_VAREJO = new LeitorAtacadoVarejo();

    private LeitoresPdf() {}

    public static LeitorPdf para(LayoutPdf layout) {
        return switch (layout) {
            case TABELA_REFERENCIA -> TABELA_REFERENCIA;
            case ATACADO_VAREJO -> ATACADO_VAREJO;
        };
    }
}
