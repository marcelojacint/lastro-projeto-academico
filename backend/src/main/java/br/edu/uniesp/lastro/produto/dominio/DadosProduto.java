package br.edu.uniesp.lastro.produto.dominio;

import java.math.BigDecimal;

public record DadosProduto(
        String codigo,
        String nome,
        String categoria,
        String unidade,
        BigDecimal precoCompra,
        BigDecimal precoVenda,
        BigDecimal estoqueAtual,
        BigDecimal estoqueMinimo,
        String status) {

    public String codigoInformado() {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.strip();
    }
}
