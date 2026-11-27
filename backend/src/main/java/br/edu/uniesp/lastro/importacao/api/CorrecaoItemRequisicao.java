package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.importacao.dominio.CorrecaoItem;
import java.math.BigDecimal;

public record CorrecaoItemRequisicao(String referencia, String descricao, String unidade, BigDecimal preco) {

    CorrecaoItem paraCorrecao() {
        return new CorrecaoItem(referencia, descricao, unidade, preco);
    }
}
