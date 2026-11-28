package br.edu.uniesp.lastro.precificacao.api;

import br.edu.uniesp.lastro.importacao.api.ImportacaoResposta;
import br.edu.uniesp.lastro.importacao.dominio.ApresentacaoFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.precificacao.aplicacao.ConsultaComparacaoService.ComparacaoImportacao;
import br.edu.uniesp.lastro.precificacao.aplicacao.ConsultaComparacaoService.ItemAusente;
import br.edu.uniesp.lastro.precificacao.aplicacao.ConsultaComparacaoService.ItemComparado;
import br.edu.uniesp.lastro.precificacao.dominio.Classificacao;
import br.edu.uniesp.lastro.precificacao.dominio.ComparacaoPreco;
import br.edu.uniesp.lastro.produto.dominio.Produto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ComparacaoResposta(
        ImportacaoResposta importacao,
        Map<Classificacao, Long> totais,
        List<ItemComparadoResposta> itens,
        List<ItemAusenteResposta> ausentes) {

    static ComparacaoResposta de(ComparacaoImportacao comparacao) {
        return new ComparacaoResposta(
                ImportacaoResposta.de(comparacao.importacao()),
                comparacao.totais(),
                comparacao.itens().stream().map(ItemComparadoResposta::de).toList(),
                comparacao.ausentes().stream().map(ItemAusenteResposta::de).toList());
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

    public record ItemAusenteResposta(
            Long apresentacaoId,
            String referencia,
            String descricao,
            String unidade,
            ModalidadePreco modalidadePreco,
            Long produtoId,
            String produtoNome,
            BigDecimal ultimoPrecoValido) {

        static ItemAusenteResposta de(ItemAusente ausente) {
            ApresentacaoFornecedor apresentacao = ausente.ausente().apresentacao();
            Produto produto = ausente.produto();
            return new ItemAusenteResposta(
                    apresentacao.getId(),
                    apresentacao.getReferencia(),
                    apresentacao.getDescricaoOriginal(),
                    apresentacao.getUnidade(),
                    apresentacao.getModalidadePreco(),
                    produto == null ? null : produto.getId(),
                    produto == null ? null : produto.getNome(),
                    ausente.ausente().ausencia().getUltimoPrecoValido());
        }
    }
}
