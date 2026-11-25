package br.edu.uniesp.lastro.fornecedor.dominio;

public record DadosFornecedor(String nome, String cnpj, String contato, String layoutPdf, Boolean ativo) {

    public String cnpjInformado() {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        return cnpj.replaceAll("\\D", "");
    }
}
