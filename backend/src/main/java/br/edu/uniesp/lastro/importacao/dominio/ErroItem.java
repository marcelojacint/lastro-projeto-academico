package br.edu.uniesp.lastro.importacao.dominio;

public enum ErroItem {
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
}
