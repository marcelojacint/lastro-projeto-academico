package br.edu.uniesp.lastro.compartilhado.notificacao;

import java.util.ArrayList;
import java.util.List;

public class Notificacao {

    private final List<MensagemErro> erros = new ArrayList<>();

    public void adicionar(String codigo, String mensagem) {
        erros.add(new MensagemErro(codigo, mensagem));
    }

    public boolean possuiErros() {
        return !erros.isEmpty();
    }

    public List<MensagemErro> getErros() {
        return List.copyOf(erros);
    }
}
