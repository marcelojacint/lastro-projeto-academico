package br.edu.uniesp.lastro.fornecedor.dominio;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ValidacaoFornecedor {

    static final int TAMANHO_MAXIMO_NOME = 200;
    static final int TAMANHO_MAXIMO_CONTATO = 200;
    static final int DIGITOS_CNPJ = 14;

    private final List<ErroCampo> erros = new ArrayList<>();

    String nome(String valor) {
        String texto = textoOpcional("nome", "Nome", valor, TAMANHO_MAXIMO_NOME);
        if (texto == null) {
            erros.add(new ErroCampo("nome", "Nome é obrigatório"));
        }
        return texto;
    }

    String contato(String valor) {
        return textoOpcional("contato", "Contato", valor, TAMANHO_MAXIMO_CONTATO);
    }

    String cnpj(DadosFornecedor dados) {
        String digitos = dados.cnpjInformado();
        if (digitos == null) {
            return null;
        }
        if (digitos.length() != DIGITOS_CNPJ || !dados.cnpj().matches("[\\d./\\-\\s]+")) {
            erros.add(new ErroCampo("cnpj", "CNPJ deve ter " + DIGITOS_CNPJ + " dígitos"));
        }
        return digitos;
    }

    LayoutPdf layoutPdf(String valor) {
        if (valor == null || valor.isBlank()) {
            erros.add(new ErroCampo("layoutPdf", "Layout do PDF é obrigatório"));
            return null;
        }
        try {
            return LayoutPdf.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException excecao) {
            erros.add(new ErroCampo("layoutPdf", "Layout do PDF deve ser TABELA_REFERENCIA ou ATACADO_VAREJO"));
            return null;
        }
    }

    void lancarSeHouverErros() {
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    private String textoOpcional(String campo, String rotulo, String valor, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.strip();
        if (texto.length() > tamanhoMaximo) {
            erros.add(new ErroCampo(campo, rotulo + " deve ter no máximo " + tamanhoMaximo + " caracteres"));
        }
        return texto;
    }
}
