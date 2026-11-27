package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

final class LinhasDaTabela {

    static final int TAMANHO_MAXIMO_LINHA = 1000;
    static final int TAMANHO_MAXIMO_DESCRICAO = 300;

    private LinhasDaTabela() {}

    static List<ItemExtraido> ler(
            List<String> linhas, LayoutPdf layout, BiFunction<Integer, String, List<ItemExtraido>> lerLinha) {
        int primeiraLinhaDeItem = DetectorCabecalho.detectar(linhas)
                .filter(cabecalho -> cabecalho.layout() == layout)
                .map(cabecalho -> cabecalho.indice() + 1)
                .orElseThrow(() -> new FalhaLeituraException(
                        MotivoFalha.LAYOUT_NAO_RECONHECIDO, "Cabeçalho do layout " + layout + " não encontrado"));
        List<ItemExtraido> itens = new ArrayList<>();
        for (int indice = primeiraLinhaDeItem; indice < linhas.size(); indice++) {
            String linha = linhas.get(indice);
            if (!linha.isBlank()) {
                itens.addAll(lerLinha.apply(indice + 1, cortar(linha)));
            }
        }
        return itens;
    }

    static List<String> tokens(String linha) {
        return Arrays.stream(linha.replace("R$", " ").strip().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .toList();
    }

    static ItemExtraido foraDoPadrao(int numeroLinha, String linha) {
        return new ItemExtraido(
                numeroLinha,
                linha,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(ErroItem.LINHA_FORA_DO_PADRAO.paraMensagem()));
    }

    static ItemExtraido item(
            int numeroLinha,
            String linha,
            String referencia,
            String descricao,
            String unidade,
            ModalidadePreco modalidade,
            String textoPreco) {
        Optional<BigDecimal> preco = ConversorPrecoBrasileiro.converter(textoPreco);
        List<MensagemErro> erros =
                preco.isPresent() ? List.of() : List.of(ErroItem.PRECO_ILEGIVEL.paraMensagem(textoPreco));
        return new ItemExtraido(
                numeroLinha, linha, referencia, descricao, unidade, modalidade, null, preco.orElse(null), erros);
    }

    private static String cortar(String linha) {
        return linha.length() > TAMANHO_MAXIMO_LINHA ? linha.substring(0, TAMANHO_MAXIMO_LINHA) : linha;
    }
}
