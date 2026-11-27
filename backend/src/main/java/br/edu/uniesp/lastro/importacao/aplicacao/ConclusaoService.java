package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import br.edu.uniesp.lastro.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoRepository;
import br.edu.uniesp.lastro.importacao.infraestrutura.ItemImportadoRepository;
import br.edu.uniesp.lastro.precificacao.dominio.HistoricoPreco;
import br.edu.uniesp.lastro.precificacao.infraestrutura.HistoricoPrecoRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConclusaoService {

    private final ImportacaoRepository importacoes;
    private final ItemImportadoRepository itens;
    private final HistoricoPrecoRepository historicos;
    private final FornecedorService fornecedorService;
    private final ObjectProvider<ComparacaoNaConclusao> comparacoes;

    public ConclusaoService(
            ImportacaoRepository importacoes,
            ItemImportadoRepository itens,
            HistoricoPrecoRepository historicos,
            FornecedorService fornecedorService,
            ObjectProvider<ComparacaoNaConclusao> comparacoes) {
        this.importacoes = importacoes;
        this.itens = itens;
        this.historicos = historicos;
        this.fornecedorService = fornecedorService;
        this.comparacoes = comparacoes;
    }

    @Transactional
    public ImportacaoComFornecedor concluir(UUID importacaoId) {
        Importacao importacao = buscarComBloqueio(importacaoId);
        Instant agora = Instant.now();
        List<ItemImportado> lidos = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(importacaoId);
        List<ItemImportado> validos = lidos.stream()
                .filter(item -> item.getSituacao() == SituacaoItem.VALIDO)
                .toList();
        importacao.concluir(validos.size() < lidos.size(), agora);
        exigirUmPrecoPorApresentacao(validos);
        comparacoes.orderedStream().forEach(comparacao -> comparacao.compararAntesDoHistorico(importacao, lidos));
        historicos.saveAll(
                validos.stream().map(item -> HistoricoPreco.doItem(item, agora)).toList());
        return comFornecedor(importacao);
    }

    @Transactional
    public ImportacaoComFornecedor descartar(UUID importacaoId) {
        Importacao importacao = buscarComBloqueio(importacaoId);
        importacao.descartar();
        return comFornecedor(importacao);
    }

    private static void exigirUmPrecoPorApresentacao(List<ItemImportado> validos) {
        Map<Long, ItemImportado> porApresentacao = new HashMap<>();
        for (ItemImportado item : validos) {
            ItemImportado anterior = porApresentacao.putIfAbsent(item.getApresentacaoId(), item);
            if (anterior != null) {
                throw new ConflitoException(
                        "itens",
                        "As linhas " + anterior.getNumeroLinha() + " e " + item.getNumeroLinha()
                                + " têm o mesmo item do fornecedor; corrija uma delas antes de concluir");
            }
        }
    }

    private ImportacaoComFornecedor comFornecedor(Importacao importacao) {
        return new ImportacaoComFornecedor(
                importacao,
                fornecedorService.buscar(importacao.getFornecedorId()).getNome());
    }

    private Importacao buscarComBloqueio(UUID importacaoId) {
        return importacoes
                .findComBloqueioById(importacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação " + importacaoId + " não encontrada"));
    }
}
