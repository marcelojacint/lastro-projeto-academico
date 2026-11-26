package br.edu.uniesp.lastro.compartilhado.erro;

import java.util.Map;

public class ProcessamentoFalhouException extends RuntimeException {

    private final transient ErroCampo erro;
    private final transient Map<String, Object> propriedades;

    public ProcessamentoFalhouException(String detalhe, ErroCampo erro, Map<String, Object> propriedades) {
        super(detalhe);
        this.erro = erro;
        this.propriedades = Map.copyOf(propriedades);
    }

    public ErroCampo getErro() {
        return erro;
    }

    public Map<String, Object> getPropriedades() {
        return propriedades;
    }
}
