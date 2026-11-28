package br.edu.uniesp.lastro.precificacao.api;

import br.edu.uniesp.lastro.importacao.api.ImportacaoResposta;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.precificacao.aplicacao.ConsultaComparacaoService.ComparacaoImportacao;
import br.edu.uniesp.lastro.precificacao.aplicacao.ConsultaComparacaoService.ItemComparado;
import br.edu.uniesp.lastro.precificacao.dominio.Classificacao;
import br.edu.uniesp.lastro.precificacao.dominio.ComparacaoPreco;
import br.edu.uniesp.lastro.produto.dominio.Produto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ComparacaoResposta(
        ImportacaoResposta importacao, Map<Classificacao, Long> totais, List<ItemComparadoResposta> itens) {

    static ComparacaoResposta de(ComparacaoImportacao comparacao) {
        return new ComparacaoResposta(
                ImportacaoResposta.de(comparacao.importacao()),
                comparacao.totais(),
                comparacao.itens().stream().map(ItemComparadoResposta::de).toList());
    }

    public record ItemComparadoResposta(
            Long itemId,
            int numeroLinha,
            String referencia,
            String descricao,
            String unidade,
            ModalidadePreco modalidadePreco,
            Long produtoId,
            String produtoNome,
            BigDecimal precoVenda,
            BigDecimal precoAnterior,
            BigDecimal precoAtual,
            BigDecimal diferenca,
            BigDecimal variacaoPercentual,
            Classificacao classificacao,
            List<String> motivosPendencia) {

        static ItemComparadoResposta de(ItemComparado comparado) {
            ItemImportado item = comparado.comparada().item();
            ComparacaoPreco comparacao = comparado.comparada().comparacao();
            Produto produto = comparado.produto();
            return new ItemComparadoResposta(
                    item.getId(),
                    item.getNumeroLinha(),
                    item.getReferencia(),
                    item.getDescricao(),
                    item.getUnidade(),
                    item.getModalidadePreco(),
                    produto == null ? null : produto.getId(),
                    produto == null ? null : produto.getNome(),
                    produto == null ? null : produto.getPrecoVenda(),
                    comparacao.getPrecoAnterior(),
                    comparacao.getPrecoAtual(),
                    comparacao.getDiferenca(),
                    comparacao.getVariacaoPercentual(),
                    comparacao.getClassificacao(),
                    comparado.motivosPendencia());
        }
    }
}
