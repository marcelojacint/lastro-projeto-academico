package br.edu.uniesp.lastro.produto.api;

import br.edu.uniesp.lastro.produto.dominio.DadosProduto;
import java.math.BigDecimal;

public record ProdutoRequisicao(
        String codigo,
        String nome,
        String categoria,
        String unidade,
        BigDecimal precoCompra,
        BigDecimal precoVenda,
        BigDecimal estoqueAtual,
        BigDecimal estoqueMinimo,
        String status) {

    DadosProduto paraDados() {
        return new DadosProduto(
                codigo, nome, categoria, unidade, precoCompra, precoVenda, estoqueAtual, estoqueMinimo, status);
    }
}
