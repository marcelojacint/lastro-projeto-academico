package br.edu.uniesp.lastro.importacao.dominio;

import java.util.Optional;

public record DecisaoAssociacao(Long produtoId, boolean criarVinculoAutomatico) {

    public static Optional<DecisaoAssociacao> decidir(
            Optional<Long> produtoDoVinculoAtivo, String descricao, CatalogoProdutos catalogo) {
        if (produtoDoVinculoAtivo.isPresent()) {
            return Optional.of(new DecisaoAssociacao(produtoDoVinculoAtivo.get(), false));
        }
        return catalogo.produtoComNomeExato(descricao).map(produtoId -> new DecisaoAssociacao(produtoId, true));
    }
}
