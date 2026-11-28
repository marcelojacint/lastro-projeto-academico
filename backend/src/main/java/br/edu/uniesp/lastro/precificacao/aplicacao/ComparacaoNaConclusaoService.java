package br.edu.uniesp.lastro.precificacao.aplicacao;

import br.edu.uniesp.lastro.importacao.aplicacao.ComparacaoNaConclusao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.precificacao.dominio.ClassificadorPreco;
import br.edu.uniesp.lastro.precificacao.dominio.ComparacaoPreco;
import br.edu.uniesp.lastro.precificacao.dominio.HistoricoPreco;
import br.edu.uniesp.lastro.precificacao.dominio.ResultadoComparacao;
import br.edu.uniesp.lastro.precificacao.infraestrutura.ComparacaoPrecoRepository;
import br.edu.uniesp.lastro.precificacao.infraestrutura.HistoricoPrecoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ComparacaoNaConclusaoService implements ComparacaoNaConclusao {

    private final HistoricoPrecoRepository historicos;
    private final ComparacaoPrecoRepository comparacoes;

    public ComparacaoNaConclusaoService(HistoricoPrecoRepository historicos, ComparacaoPrecoRepository comparacoes) {
        this.historicos = historicos;
        this.comparacoes = comparacoes;
    }

    @Override
    public void compararAntesDoHistorico(Importacao importacao, List<ItemImportado> itens) {
        Instant agora = Instant.now();
        comparacoes.saveAll(itens.stream()
                .map(item -> ComparacaoPreco.registrar(
                        importacao.getId(), item.getId(), comparar(item, importacao.getId()), agora))
                .toList());
    }

    private ResultadoComparacao comparar(ItemImportado item, UUID importacaoAtual) {
        return ClassificadorPreco.classificar(
                buscarPrecoAnterior(item, importacaoAtual), item.getPreco(), item.getSituacao() == SituacaoItem.VALIDO);
    }

    private BigDecimal buscarPrecoAnterior(ItemImportado item, UUID importacaoAtual) {
        if (item.getApresentacaoId() == null) {
            return null;
        }
        return historicos
                .buscarUltimoPrecoValido(item.getApresentacaoId(), importacaoAtual)
                .map(HistoricoPreco::getPreco)
                .orElse(null);
    }
}
