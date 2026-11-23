package br.edu.uniesp.lastro.compartilhado.erro;

public class ConflitoException extends RuntimeException {

    private final transient ErroCampo erro;

    public ConflitoException(String campo, String mensagem) {
        super(mensagem);
        this.erro = new ErroCampo(campo, mensagem);
    }

    public ErroCampo getErro() {
        return erro;
    }
}
