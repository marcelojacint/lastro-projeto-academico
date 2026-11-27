package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import java.util.List;

public interface ComparacaoNaConclusao {

    void compararAntesDoHistorico(Importacao importacao, List<ItemImportado> itens);
}
