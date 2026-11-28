package br.edu.uniesp.lastro.precificacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.precificacao.dominio.ComparacaoPreco;

public record ComparacaoComItem(ComparacaoPreco comparacao, ItemImportado item) {}
