package br.edu.uniesp.lastro.precificacao.aplicacao;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import br.edu.uniesp.lastro.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.importacao.aplicacao.ImportacaoComFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.dominio.StatusImportacao;
import br.edu.uniesp.lastro.importacao.dominio.VinculoProduto;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoRepository;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoProdutoRepository;
import br.edu.uniesp.lastro.precificacao.dominio.Classificacao;
import br.edu.uniesp.lastro.precificacao.infraestrutura.ComparacaoComItem;
import br.edu.uniesp.lastro.precificacao.infraestrutura.ComparacaoPrecoRepository;
import br.edu.uniesp.lastro.produto.aplicacao.ProdutoService;
import br.edu.uniesp.lastro.produto.dominio.Produto;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaComparacaoService {

    private static final Set<StatusImportacao> CONCLUIDAS =
            EnumSet.of(StatusImportacao.CONCLUIDA, StatusImportacao.CONCLUIDA_COM_PENDENCIAS);
    private static final String SEM_PRODUTO = "Item sem produto associado";

    private final ImportacaoRepository importacoes;
    private final ComparacaoPrecoRepository comparacoes;
    private final VinculoProdutoRepository vinculos;
    private final FornecedorService fornecedorService;
    private final ProdutoService produtoService;

    public ConsultaComparacaoService(
            ImportacaoRepository importacoes,
            ComparacaoPrecoRepository comparacoes,
            VinculoProdutoRepository vinculos,
            FornecedorService fornecedorService,
            ProdutoService produtoService) {
        this.importacoes = importacoes;
        this.comparacoes = comparacoes;
        this.vinculos = vinculos;
        this.fornecedorService = fornecedorService;
        this.produtoService = produtoService;
    }

    @Transactional(readOnly = true)
    public ComparacaoImportacao consultar(UUID importacaoId, Classificacao filtro) {
        Importacao importacao = importacoes
                .findById(importacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação " + importacaoId + " não encontrada"));
        if (!CONCLUIDAS.contains(importacao.getStatus())) {
            throw new ConflitoException("status", "A comparação só existe depois que a importação é concluída");
        }
        List<ComparacaoComItem> todas = comparacoes.listarComItens(importacaoId);
        Map<Long, Produto> produtoPorApresentacao = produtosPorApresentacao(todas);
        List<ItemComparado> itens = todas.stream()
                .filter(comparada -> filtro == null || comparada.comparacao().getClassificacao() == filtro)
                .map(comparada -> new ItemComparado(
                        comparada,
                        produtoPorApresentacao.get(comparada.item().getApresentacaoId()),
                        motivosPendencia(comparada)))
                .toList();
        String fornecedorNome =
                fornecedorService.buscar(importacao.getFornecedorId()).getNome();
        return new ComparacaoImportacao(new ImportacaoComFornecedor(importacao, fornecedorNome), contar(todas), itens);
    }

    private Map<Long, Produto> produtosPorApresentacao(List<ComparacaoComItem> comparadas) {
        List<Long> apresentacoes = comparadas.stream()
                .map(comparada -> comparada.item().getApresentacaoId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Long> produtoIdPorApresentacao = vinculos.findByApresentacaoIdInAndAtivoTrue(apresentacoes).stream()
                .collect(Collectors.toMap(VinculoProduto::getApresentacaoId, VinculoProduto::getProdutoId));
        Map<Long, Produto> produtos = produtoService.porId(produtoIdPorApresentacao.values());
        return produtoIdPorApresentacao.entrySet().stream()
                .filter(entrada -> produtos.containsKey(entrada.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entrada -> produtos.get(entrada.getValue())));
    }

    private static List<String> motivosPendencia(ComparacaoComItem comparada) {
        if (comparada.comparacao().getClassificacao() != Classificacao.PENDENTE) {
            return List.of();
        }
        ItemImportado item = comparada.item();
        List<String> motivos =
                item.getErros().stream().map(MensagemErro::mensagem).toList();
        if (motivos.isEmpty() && item.getSituacao() == SituacaoItem.PENDENTE_ASSOCIACAO) {
            return List.of(SEM_PRODUTO);
        }
        return motivos;
    }

    private static Map<Classificacao, Long> contar(List<ComparacaoComItem> comparadas) {
        Map<Classificacao, Long> totais = new EnumMap<>(Classificacao.class);
        for (Classificacao classificacao : Classificacao.values()) {
            totais.put(classificacao, 0L);
        }
        comparadas.forEach(comparada -> totais.merge(comparada.comparacao().getClassificacao(), 1L, Long::sum));
        return totais;
    }

    public record ItemComparado(ComparacaoComItem comparada, Produto produto, List<String> motivosPendencia) {}

    public record ComparacaoImportacao(
            ImportacaoComFornecedor importacao, Map<Classificacao, Long> totais, List<ItemComparado> itens) {}
}
