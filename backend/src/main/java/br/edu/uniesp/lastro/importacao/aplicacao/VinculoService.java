package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoAtivo;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoProdutoRepository;
import br.edu.uniesp.lastro.produto.aplicacao.ProdutoService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VinculoService {

    private final VinculoProdutoRepository vinculos;
    private final FornecedorService fornecedorService;
    private final ProdutoService produtoService;

    public VinculoService(
            VinculoProdutoRepository vinculos, FornecedorService fornecedorService, ProdutoService produtoService) {
        this.vinculos = vinculos;
        this.fornecedorService = fornecedorService;
        this.produtoService = produtoService;
    }

    @Transactional(readOnly = true)
    public List<VinculoComProduto> listarAtivos(Long fornecedorId) {
        fornecedorService.buscar(fornecedorId);
        List<VinculoAtivo> ativos = vinculos.listarAtivosDoFornecedor(fornecedorId);
        Map<Long, String> nomes = produtoService.nomesPorId(
                ativos.stream().map(VinculoAtivo::produtoId).distinct().toList());
        return ativos.stream()
                .map(vinculo -> new VinculoComProduto(vinculo, nomes.get(vinculo.produtoId())))
                .toList();
    }

    public record VinculoComProduto(VinculoAtivo vinculo, String produtoNome) {}
}
