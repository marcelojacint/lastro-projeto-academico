package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.importacao.dominio.CatalogoProdutos;
import br.edu.uniesp.lastro.importacao.dominio.CatalogoProdutos.ProdutoDoCatalogo;
import br.edu.uniesp.lastro.importacao.dominio.DecisaoAssociacao;
import br.edu.uniesp.lastro.importacao.dominio.VinculoProduto;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoProdutoRepository;
import br.edu.uniesp.lastro.produto.aplicacao.ProdutoService;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AssociacaoProdutos {

    private final ProdutoService produtoService;
    private final VinculoProdutoRepository vinculos;

    public AssociacaoProdutos(ProdutoService produtoService, VinculoProdutoRepository vinculos) {
        this.produtoService = produtoService;
        this.vinculos = vinculos;
    }

    public CatalogoProdutos carregarCatalogo() {
        return CatalogoProdutos.dosProdutosAtivos(produtoService.listarAtivos().stream()
                .map(produto -> new ProdutoDoCatalogo(produto.getId(), produto.getNome()))
                .toList());
    }

    public boolean associar(Long apresentacaoId, String descricao, CatalogoProdutos catalogo, Instant agora) {
        if (apresentacaoId == null) {
            return false;
        }
        Optional<DecisaoAssociacao> decisao = DecisaoAssociacao.decidir(
                vinculos.findByApresentacaoIdAndAtivoTrue(apresentacaoId).map(VinculoProduto::getProdutoId),
                descricao,
                catalogo);
        decisao.filter(DecisaoAssociacao::criarVinculoAutomatico)
                .ifPresent(nova ->
                        vinculos.saveAndFlush(VinculoProduto.automatico(apresentacaoId, nova.produtoId(), agora)));
        return decisao.isPresent();
    }
}
