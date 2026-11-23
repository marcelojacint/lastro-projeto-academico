package br.edu.uniesp.lastro.produto.dominio;

public record FiltroProduto(String nome, String categoria, StatusProduto status) {

    public FiltroProduto {
        nome = textoInformado(nome);
        categoria = textoInformado(categoria);
    }

    private static String textoInformado(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
