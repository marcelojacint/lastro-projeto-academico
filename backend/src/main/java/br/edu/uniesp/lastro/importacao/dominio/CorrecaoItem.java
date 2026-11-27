package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record CorrecaoItem(String referencia, String descricao, String unidade, BigDecimal preco) {

    static final int TAMANHO_MAXIMO_REFERENCIA = 50;
    static final int TAMANHO_MAXIMO_DESCRICAO = 300;
    static final int TAMANHO_MAXIMO_UNIDADE = 10;
    static final int CASAS_DECIMAIS_PRECO = 2;
    static final BigDecimal PRECO_MAXIMO = new BigDecimal("9999999999.99");

    public ItemExtraido sobre(ItemImportado item) {
        List<ErroCampo> erros = new ArrayList<>();
        String referenciaCorrigida = texto(referencia, "referencia", "Referência", TAMANHO_MAXIMO_REFERENCIA, erros);
        String descricaoCorrigida = texto(descricao, "descricao", "Descrição", TAMANHO_MAXIMO_DESCRICAO, erros);
        String unidadeCorrigida = texto(unidade, "unidade", "Unidade", TAMANHO_MAXIMO_UNIDADE, erros);
        if (preco != null && preco.stripTrailingZeros().scale() > CASAS_DECIMAIS_PRECO) {
            erros.add(new ErroCampo("preco", "Preço deve ter no máximo " + CASAS_DECIMAIS_PRECO + " casas decimais"));
        }
        if (preco != null && preco.compareTo(PRECO_MAXIMO) > 0) {
            erros.add(new ErroCampo("preco", "Preço acima do valor máximo aceito"));
        }
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
        return new ItemExtraido(
                item.getNumeroLinha(),
                item.getLinhaOriginal(),
                referenciaCorrigida,
                descricaoCorrigida,
                unidadeCorrigida == null ? null : unidadeCorrigida.toUpperCase(Locale.ROOT),
                item.getModalidadePreco(),
                item.getQuantidade(),
                preco,
                List.of());
    }

    private static String texto(String valor, String campo, String rotulo, int tamanhoMaximo, List<ErroCampo> erros) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.strip().replaceAll("\\s+", " ");
        if (texto.length() > tamanhoMaximo) {
            erros.add(new ErroCampo(campo, rotulo + " deve ter no máximo " + tamanhoMaximo + " caracteres"));
        }
        return texto;
    }
}
