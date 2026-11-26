package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import java.math.BigDecimal;
import java.util.List;

public record ItemExtraido(
        int numeroLinha,
        String linhaOriginal,
        String referencia,
        String descricao,
        String unidade,
        ModalidadePreco modalidadePreco,
        BigDecimal quantidade,
        BigDecimal preco,
        List<MensagemErro> errosDeLeitura) {

    public ItemExtraido {
        errosDeLeitura = errosDeLeitura == null ? List.of() : List.copyOf(errosDeLeitura);
    }
}
