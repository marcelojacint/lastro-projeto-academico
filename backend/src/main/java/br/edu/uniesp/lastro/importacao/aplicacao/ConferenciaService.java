package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.importacao.dominio.CorrecaoItem;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemExtraido;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.dominio.VinculoProduto;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoRepository;
import br.edu.uniesp.lastro.importacao.infraestrutura.ItemImportadoRepository;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoProdutoRepository;
import br.edu.uniesp.lastro.produto.aplicacao.ProdutoService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConferenciaService {

    private final ImportacaoRepository importacoes;
    private final ItemImportadoRepository itens;
    private final VinculoProdutoRepository vinculos;
    private final FornecedorService fornecedorService;
    private final ProdutoService produtoService;
    private final ResolucaoApresentacao resolucao;
    private final AssociacaoProdutos associacao;

    public ConferenciaService(
            ImportacaoRepository importacoes,
            ItemImportadoRepository itens,
            VinculoProdutoRepository vinculos,
            FornecedorService fornecedorService,
            ProdutoService produtoService,
            ResolucaoApresentacao resolucao,
            AssociacaoProdutos associacao) {
        this.importacoes = importacoes;
        this.itens = itens;
        this.vinculos = vinculos;
        this.fornecedorService = fornecedorService;
        this.produtoService = produtoService;
        this.resolucao = resolucao;
        this.associacao = associacao;
    }

    @Transactional(readOnly = true)
    public DetalheImportacao detalhar(UUID importacaoId) {
        Importacao importacao = buscar(importacaoId);
        Fornecedor fornecedor = fornecedorService.buscar(importacao.getFornecedorId());
        List<ItemImportado> lidos = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(importacaoId);
        List<Long> apresentacoes = lidos.stream()
                .map(ItemImportado::getApresentacaoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Long> produtoPorApresentacao = vinculos.findByApresentacaoIdInAndAtivoTrue(apresentacoes).stream()
                .collect(Collectors.toMap(VinculoProduto::getApresentacaoId, VinculoProduto::getProdutoId));
        Map<Long, String> nomes = produtoService.nomesPorId(produtoPorApresentacao.values());
        List<ItemConferencia> conferidos = lidos.stream()
                .map(item -> {
                    Long produtoId = item.getApresentacaoId() == null
                            ? null
                            : produtoPorApresentacao.get(item.getApresentacaoId());
                    return new ItemConferencia(item, produtoId, produtoId == null ? null : nomes.get(produtoId));
                })
                .toList();
        return new DetalheImportacao(new ImportacaoComFornecedor(importacao, fornecedor.getNome()), conferidos);
    }

    @Transactional
    public DetalheImportacao corrigir(UUID importacaoId, Long itemId, CorrecaoItem correcao) {
        Importacao importacao = buscar(importacaoId);
        importacao.exigirEmConferencia();
        ItemImportado item = itens.findByIdAndImportacaoId(itemId, importacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Item " + itemId + " não encontrado na importação " + importacaoId));
        Fornecedor fornecedor = fornecedorService.buscar(importacao.getFornecedorId());
        Instant agora = Instant.now();
        ItemExtraido corrigido = correcao.sobre(item);
        Long apresentacaoId = resolucao.resolver(fornecedor.getId(), corrigido, agora);
        boolean associada =
                associacao.associar(apresentacaoId, corrigido.descricao(), associacao.carregarCatalogo(), agora);
        item.corrigir(corrigido, fornecedor.getLayoutPdf(), apresentacaoId, associada);
        itens.flush();
        List<ItemImportado> todos = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(importacaoId);
        long validos = todos.stream()
                .filter(lido -> lido.getSituacao() == SituacaoItem.VALIDO)
                .count();
        importacao.atualizarContadores(todos.size(), (int) validos);
        return detalhar(importacaoId);
    }

    private Importacao buscar(UUID importacaoId) {
        return importacoes
                .findById(importacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação " + importacaoId + " não encontrada"));
    }

    public record ItemConferencia(ItemImportado item, Long produtoId, String produtoNome) {}

    public record DetalheImportacao(ImportacaoComFornecedor importacao, List<ItemConferencia> itens) {}
}
