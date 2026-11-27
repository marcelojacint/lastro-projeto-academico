package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.util.List;

public class LeitorAtacadoVarejo implements LeitorPdf {

    private static final int TOKENS_MINIMOS = 3;
    private static final String TEM_DIGITO = ".*\\d.*";

    @Override
    public LayoutPdf layout() {
        return LayoutPdf.ATACADO_VAREJO;
    }

    @Override
    public List<ItemExtraido> ler(List<String> linhas) {
        return LinhasDaTabela.ler(linhas, layout(), this::lerLinha);
    }

    private List<ItemExtraido> lerLinha(int numeroLinha, String linha) {
        List<String> tokens = LinhasDaTabela.tokens(linha);
        int total = tokens.size();
        if (total < TOKENS_MINIMOS
                || !tokens.get(total - 2).matches(TEM_DIGITO)
                || !tokens.get(total - 1).matches(TEM_DIGITO)) {
            return List.of(LinhasDaTabela.foraDoPadrao(numeroLinha, linha));
        }
        String descricao = String.join(" ", tokens.subList(0, total - 2));
        if (descricao.length() > LinhasDaTabela.TAMANHO_MAXIMO_DESCRICAO) {
            return List.of(LinhasDaTabela.foraDoPadrao(numeroLinha, linha));
        }
        return List.of(
                LinhasDaTabela.item(
                        numeroLinha, linha, null, descricao, null, ModalidadePreco.ATACADO, tokens.get(total - 2)),
                LinhasDaTabela.item(
                        numeroLinha, linha, null, descricao, null, ModalidadePreco.VAREJO, tokens.get(total - 1)));
    }
}
