package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.importacao.aplicacao.ConferenciaService.DetalheImportacao;
import br.edu.uniesp.lastro.importacao.aplicacao.ConferenciaService.ItemConferencia;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import java.math.BigDecimal;
import java.util.List;

public record ImportacaoDetalheResposta(ImportacaoResposta importacao, List<ItemConferenciaResposta> itens) {

    static ImportacaoDetalheResposta de(DetalheImportacao detalhe) {
        return new ImportacaoDetalheResposta(
                ImportacaoResposta.de(detalhe.importacao()),
                detalhe.itens().stream().map(ItemConferenciaResposta::de).toList());
    }

    public record ItemConferenciaResposta(
            Long id,
            int numeroLinha,
            String linhaOriginal,
            String referencia,
            String descricao,
            String unidade,
            ModalidadePreco modalidadePreco,
            BigDecimal quantidade,
            BigDecimal preco,
            SituacaoItem situacao,
            List<MensagemErro> erros,
            Long apresentacaoId,
            Long produtoId,
            String produtoNome) {

        static ItemConferenciaResposta de(ItemConferencia conferido) {
            ItemImportado item = conferido.item();
            return new ItemConferenciaResposta(
                    item.getId(),
                    item.getNumeroLinha(),
                    item.getLinhaOriginal(),
                    item.getReferencia(),
                    item.getDescricao(),
                    item.getUnidade(),
                    item.getModalidadePreco(),
                    item.getQuantidade(),
                    item.getPreco(),
                    item.getSituacao(),
                    item.getErros(),
                    item.getApresentacaoId(),
                    conferido.produtoId(),
                    conferido.produtoNome());
        }
    }
}
