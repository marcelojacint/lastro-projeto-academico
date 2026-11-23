package br.edu.uniesp.lastro.produto.api;

import br.edu.uniesp.lastro.produto.dominio.Produto;
import br.edu.uniesp.lastro.produto.dominio.StatusProduto;
import java.math.BigDecimal;
import java.time.Instant;

public record ProdutoResposta(
        Long id,
        String codigo,
        String nome,
        String categoria,
        String unidade,
        BigDecimal precoCompra,
        BigDecimal precoVenda,
        BigDecimal estoqueAtual,
        BigDecimal estoqueMinimo,
        StatusProduto status,
        Instant criadoEm,
        Instant atualizadoEm) {

    static ProdutoResposta de(Produto produto) {
        return new ProdutoResposta(
                produto.getId(),
                produto.getCodigo(),
                produto.getNome(),
                produto.getCategoria(),
                produto.getUnidade(),
                produto.getPrecoCompra(),
                produto.getPrecoVenda(),
                produto.getEstoqueAtual(),
                produto.getEstoqueMinimo(),
                produto.getStatus(),
                produto.getCriadoEm(),
                produto.getAtualizadoEm());
    }
}
