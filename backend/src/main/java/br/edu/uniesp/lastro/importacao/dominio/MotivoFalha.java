package br.edu.uniesp.lastro.importacao.dominio;

public enum MotivoFalha {
    LAYOUT_NAO_RECONHECIDO("O PDF não segue o layout cadastrado para o fornecedor"),
    PDF_ILEGIVEL("Não foi possível ler o texto do PDF"),
    ERRO_INTERNO("Erro interno ao ler o PDF");

    private final String mensagem;

    MotivoFalha(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
