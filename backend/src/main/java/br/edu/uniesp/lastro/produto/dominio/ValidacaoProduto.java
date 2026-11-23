package br.edu.uniesp.lastro.produto.dominio;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ValidacaoProduto {

    static final int TAMANHO_MAXIMO_CODIGO = 50;
    static final int TAMANHO_MAXIMO_NOME = 200;
    static final int TAMANHO_MAXIMO_CATEGORIA = 100;
    static final int TAMANHO_MAXIMO_UNIDADE = 20;
    private static final int CASAS_DECIMAIS_PRECO = 2;
    private static final int DIGITOS_INTEIROS_PRECO = 10;
    private static final int CASAS_DECIMAIS_ESTOQUE = 3;
    private static final int DIGITOS_INTEIROS_ESTOQUE = 9;

    private final List<ErroCampo> erros = new ArrayList<>();

    String textoObrigatorio(String campo, String rotulo, String valor, int tamanhoMaximo) {
        String texto = textoOpcional(campo, rotulo, valor, tamanhoMaximo);
        if (texto == null) {
            erros.add(new ErroCampo(campo, rotulo + " é obrigatório"));
        }
        return texto;
    }

    String textoOpcional(String campo, String rotulo, String valor, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.strip();
        if (texto.length() > tamanhoMaximo) {
            erros.add(new ErroCampo(campo, rotulo + " deve ter no máximo " + tamanhoMaximo + " caracteres"));
        }
        return texto;
    }

    BigDecimal precoObrigatorio(String campo, String rotulo, BigDecimal valor) {
        if (valor == null) {
            erros.add(new ErroCampo(campo, rotulo + " é obrigatório"));
            return null;
        }
        return precoOpcional(campo, rotulo, valor);
    }

    BigDecimal precoOpcional(String campo, String rotulo, BigDecimal valor) {
        return numeroNaoNegativo(campo, rotulo, valor, CASAS_DECIMAIS_PRECO, DIGITOS_INTEIROS_PRECO);
    }

    BigDecimal estoque(String campo, String rotulo, BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return numeroNaoNegativo(campo, rotulo, valor, CASAS_DECIMAIS_ESTOQUE, DIGITOS_INTEIROS_ESTOQUE);
    }

    StatusProduto status(String valor, StatusProduto statusPadrao) {
        if (valor == null || valor.isBlank()) {
            return statusPadrao;
        }
        try {
            return StatusProduto.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException excecao) {
            erros.add(new ErroCampo("status", "Status deve ser ATIVO ou INATIVO"));
            return statusPadrao;
        }
    }

    void lancarSeHouverErros() {
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    private BigDecimal numeroNaoNegativo(
            String campo, String rotulo, BigDecimal valor, int casasDecimais, int digitosInteiros) {
        if (valor == null) {
            return null;
        }
        if (valor.signum() < 0) {
            erros.add(new ErroCampo(campo, rotulo + " não pode ser negativo"));
        }
        BigDecimal semZerosFinais = valor.stripTrailingZeros();
        if (semZerosFinais.scale() > casasDecimais) {
            erros.add(new ErroCampo(campo, rotulo + " deve ter no máximo " + casasDecimais + " casas decimais"));
        }
        if (semZerosFinais.precision() - semZerosFinais.scale() > digitosInteiros) {
            erros.add(new ErroCampo(campo, rotulo + " excede o valor máximo permitido"));
        }
        return valor;
    }
}
