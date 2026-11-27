package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.util.List;
import java.util.Locale;

public class LeitorTabelaReferencia implements LeitorPdf {

    private static final int TOKENS_MINIMOS = 5;
    private static final String REFERENCIA = "\\d+";
    private static final String UNIDADE = "\\p{L}{1,5}";

    @Override
    public LayoutPdf layout() {
        return LayoutPdf.TABELA_REFERENCIA;
    }

    @Override
    public List<ItemExtraido> ler(List<String> linhas) {
        return LinhasDaTabela.ler(linhas, layout(), this::lerLinha);
    }

    private List<ItemExtraido> lerLinha(int numeroLinha, String linha) {
        List<String> tokens = LinhasDaTabela.tokens(linha);
        int total = tokens.size();
        if (total < TOKENS_MINIMOS
                || !tokens.getFirst().matches(REFERENCIA)
                || !tokens.get(total - 3).matches(UNIDADE)) {
            return List.of(LinhasDaTabela.foraDoPadrao(numeroLinha, linha));
        }
        String referencia = tokens.getFirst();
        String descricao = String.join(" ", tokens.subList(1, total - 3));
        if (descricao.length() > LinhasDaTabela.TAMANHO_MAXIMO_DESCRICAO) {
            return List.of(LinhasDaTabela.foraDoPadrao(numeroLinha, linha));
        }
        String unidade = tokens.get(total - 3).toUpperCase(Locale.ROOT);
        return List.of(
                LinhasDaTabela.item(
                        numeroLinha,
                        linha,
                        referencia,
                        descricao,
                        unidade,
                        ModalidadePreco.UNITARIO,
                        tokens.get(total - 2)),
                LinhasDaTabela.item(
                        numeroLinha,
                        linha,
                        referencia,
                        descricao,
                        unidade,
                        ModalidadePreco.FARDO,
                        tokens.get(total - 1)));
    }
}
