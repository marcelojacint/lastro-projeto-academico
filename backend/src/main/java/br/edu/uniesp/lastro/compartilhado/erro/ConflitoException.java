package br.edu.uniesp.lastro.compartilhado.erro;

import java.util.Map;

public class ConflitoException extends RuntimeException {

    private final transient ErroCampo erro;
    private final transient Map<String, Object> propriedades;

    public ConflitoException(String campo, String mensagem) {
        this(campo, mensagem, Map.of());
    }

    public ConflitoException(String campo, String mensagem, Map<String, Object> propriedades) {
        super(mensagem);
        this.erro = new ErroCampo(campo, mensagem);
        this.propriedades = Map.copyOf(propriedades);
    }

    public ErroCampo getErro() {
        return erro;
    }

    public Map<String, Object> getPropriedades() {
        return propriedades;
    }
}
