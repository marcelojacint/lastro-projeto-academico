package br.edu.uniesp.lastro.importacao.dominio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CatalogoProdutos {

    private final Map<String, List<Long>> produtosPorNome;

    private CatalogoProdutos(Map<String, List<Long>> produtosPorNome) {
        this.produtosPorNome = produtosPorNome;
    }

    public static CatalogoProdutos dosProdutosAtivos(List<ProdutoDoCatalogo> produtos) {
        Map<String, List<Long>> porNome = new HashMap<>();
        for (ProdutoDoCatalogo produto : produtos) {
            String nome = NormalizadorDescricao.normalizar(produto.nome());
            if (nome != null) {
                porNome.computeIfAbsent(nome, chave -> new ArrayList<>()).add(produto.id());
            }
        }
        return new CatalogoProdutos(porNome);
    }

    public Optional<Long> produtoComNomeExato(String descricao) {
        String nome = NormalizadorDescricao.normalizar(descricao);
        List<Long> produtos = nome == null ? List.of() : produtosPorNome.getOrDefault(nome, List.of());
        return produtos.size() == 1 ? Optional.of(produtos.getFirst()) : Optional.empty();
    }

    public record ProdutoDoCatalogo(Long id, String nome) {}
}
