package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.util.List;

public interface LeitorPdf {

    LayoutPdf layout();

    List<ItemExtraido> ler(List<String> linhas);
}
