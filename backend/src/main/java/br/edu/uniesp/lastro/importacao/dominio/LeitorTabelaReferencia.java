package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LeitorTabelaReferencia implements LeitorPdf {

    private static final int TOKENS_MINIMOS = 5;
    private static final int TAMANHO_MAXIMO_LINHA = 1000;
    private static final int TAMANHO_MAXIMO_DESCRICAO = 300;
    private static final String REFERENCIA = "\\d+";
    private static final String UNIDADE = "\\p{L}{1,5}";

    @Override
    public LayoutPdf layout() {
        return LayoutPdf.TABELA_REFERENCIA;
    }

    @Override
    public List<ItemExtraido> ler(List<String> linhas) {
        int primeiraLinhaDeItem = DetectorCabecalho.detectar(linhas)
                .filter(cabecalho -> cabecalho.layout() == layout())
                .map(cabecalho -> cabecalho.indice() + 1)
                .orElseThrow(() -> new FalhaLeituraException(
                        MotivoFalha.LAYOUT_NAO_RECONHECIDO, "Cabeçalho da tabela com referência não encontrado"));
        List<ItemExtraido> itens = new ArrayList<>();
        for (int indice = primeiraLinhaDeItem; indice < linhas.size(); indice++) {
            String linha = linhas.get(indice);
            if (!linha.isBlank()) {
                itens.addAll(lerLinha(indice + 1, cortar(linha)));
            }
        }
        return itens;
    }

    private List<ItemExtraido> lerLinha(int numeroLinha, String linha) {
        List<String> tokens = Arrays.stream(linha.replace("R$", " ").strip().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .toList();
        int total = tokens.size();
        if (total < TOKENS_MINIMOS
                || !tokens.getFirst().matches(REFERENCIA)
                || !tokens.get(total - 3).matches(UNIDADE)) {
            return List.of(foraDoPadrao(numeroLinha, linha));
        }
        String referencia = tokens.getFirst();
        String descricao = String.join(" ", tokens.subList(1, total - 3));
        if (descricao.length() > TAMANHO_MAXIMO_DESCRICAO) {
            return List.of(foraDoPadrao(numeroLinha, linha));
        }
        String unidade = tokens.get(total - 3).toUpperCase(Locale.ROOT);
        return List.of(
                item(
                        numeroLinha,
                        linha,
                        referencia,
                        descricao,
                        unidade,
                        ModalidadePreco.UNITARIO,
                        tokens.get(total - 2)),
                item(numeroLinha, linha, referencia, descricao, unidade, ModalidadePreco.FARDO, tokens.get(total - 1)));
    }

    private static ItemExtraido foraDoPadrao(int numeroLinha, String linha) {
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

    private static String cortar(String linha) {
        return linha.length() > TAMANHO_MAXIMO_LINHA ? linha.substring(0, TAMANHO_MAXIMO_LINHA) : linha;
    }

    private static ItemExtraido item(
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
}
