package br.edu.uniesp.lastro.precificacao.aplicacao;

import br.edu.uniesp.lastro.importacao.aplicacao.ComparacaoNaConclusao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.precificacao.dominio.AusenciaItem;
import br.edu.uniesp.lastro.precificacao.infraestrutura.AusenciaItemRepository;
import br.edu.uniesp.lastro.precificacao.infraestrutura.HistoricoPrecoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AusenciaNaConclusaoService implements ComparacaoNaConclusao {

    private final HistoricoPrecoRepository historicos;
    private final AusenciaItemRepository ausencias;

    public AusenciaNaConclusaoService(HistoricoPrecoRepository historicos, AusenciaItemRepository ausencias) {
        this.historicos = historicos;
        this.ausencias = ausencias;
    }

    @Override
    public void compararAntesDoHistorico(Importacao importacao, List<ItemImportado> itens) {
        Set<Long> vieramNoPdf = itens.stream()
                .map(ItemImportado::getApresentacaoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Instant agora = Instant.now();
        ausencias.saveAll(
                historicos
                        .listarUltimosPrecosValidosDoFornecedor(importacao.getFornecedorId(), importacao.getId())
                        .stream()
                        .filter(ultimo -> !vieramNoPdf.contains(ultimo.getApresentacaoId()))
                        .map(ultimo -> AusenciaItem.registrar(
                                importacao.getId(), ultimo.getApresentacaoId(), ultimo.getPreco(), agora))
                        .toList());
    }
}
