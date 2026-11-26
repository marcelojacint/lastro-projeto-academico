package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;

public enum ErroItem {
    LINHA_FORA_DO_PADRAO("Linha fora do padrão do layout"),
    PRECO_ILEGIVEL("Preço \"%s\" não é um valor válido"),
    REFERENCIA_AUSENTE("Referência não informada"),
    DESCRICAO_VAZIA("Descrição vazia"),
    UNIDADE_AUSENTE("Unidade não informada"),
    MODALIDADE_AUSENTE("Modalidade de preço não identificada"),
    PRECO_AUSENTE("Preço não informado"),
    PRECO_ZERO("Preço igual a zero"),
    PRECO_NEGATIVO("Preço negativo");

    private final String mensagem;

    ErroItem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public MensagemErro paraMensagem(Object... detalhes) {
        return new MensagemErro(name(), mensagem.formatted(detalhes));
    }
}
