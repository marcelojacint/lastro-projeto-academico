package br.edu.uniesp.lastro.precificacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.ApresentacaoFornecedor;
import br.edu.uniesp.lastro.precificacao.dominio.AusenciaItem;

public record AusenciaComApresentacao(AusenciaItem ausencia, ApresentacaoFornecedor apresentacao) {}
