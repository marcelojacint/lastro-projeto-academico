package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import java.util.List;

public record ResultadoValidacaoItem(SituacaoItem situacao, List<MensagemErro> erros) {

    public ResultadoValidacaoItem {
        erros = List.copyOf(erros);
    }

    public boolean invalido() {
        return situacao == SituacaoItem.INVALIDO;
    }

    public SituacaoItem situacaoComAssociacao(boolean apresentacaoAssociada) {
        if (invalido()) {
            return SituacaoItem.INVALIDO;
        }
        return apresentacaoAssociada ? SituacaoItem.VALIDO : SituacaoItem.PENDENTE_ASSOCIACAO;
    }
}
