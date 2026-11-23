package br.edu.uniesp.lastro.compartilhado.erro;

import java.util.List;

public class DadosInvalidosException extends RuntimeException {

    private final transient List<ErroCampo> erros;

    public DadosInvalidosException(List<ErroCampo> erros) {
        super("Dados inválidos");
        this.erros = List.copyOf(erros);
    }

    public List<ErroCampo> getErros() {
        return erros;
    }
}
