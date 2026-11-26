package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import br.edu.uniesp.lastro.importacao.dominio.ApresentacaoFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.FalhaLeituraException;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemExtraido;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.LeitorPdf;
import br.edu.uniesp.lastro.importacao.dominio.LeitoresPdf;
import br.edu.uniesp.lastro.importacao.dominio.NormalizadorDescricao;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.infraestrutura.ApresentacaoFornecedorRepository;
import br.edu.uniesp.lastro.importacao.infraestrutura.ExtratorTextoPdf;
import br.edu.uniesp.lastro.importacao.infraestrutura.ItemImportadoRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LeituraImportacao {

    private final ExtratorTextoPdf extrator;
    private final ApresentacaoFornecedorRepository apresentacoes;
    private final ItemImportadoRepository itens;

    public LeituraImportacao(
            ExtratorTextoPdf extrator, ApresentacaoFornecedorRepository apresentacoes, ItemImportadoRepository itens) {
        this.extrator = extrator;
        this.apresentacoes = apresentacoes;
        this.itens = itens;
    }

    public void ler(Importacao importacao, LayoutPdf layout, byte[] conteudo) {
        Optional<LeitorPdf> leitor = LeitoresPdf.para(layout);
        if (leitor.isEmpty()) {
            return;
        }
        try {
            List<ItemExtraido> extraidos = leitor.get().ler(extrator.extrairLinhas(conteudo));
            Instant agora = Instant.now();
            Map<String, Long> apresentacoesDaLeitura = new HashMap<>();
            List<ItemImportado> registrados = extraidos.stream()
                    .map(item -> ItemImportado.registrarValidando(
                            importacao.getId(),
                            item,
                            layout,
                            resolverApresentacao(importacao.getFornecedorId(), item, apresentacoesDaLeitura, agora)))
                    .toList();
            itens.saveAll(registrados);
            long validos = registrados.stream()
                    .filter(item -> item.getSituacao() == SituacaoItem.VALIDO)
                    .count();
            importacao.iniciarConferencia(registrados.size(), (int) validos);
        } catch (FalhaLeituraException falha) {
            importacao.falhar(falha.getMotivo());
        }
    }

    private Long resolverApresentacao(
            Long fornecedorId, ItemExtraido item, Map<String, Long> apresentacoesDaLeitura, Instant agora) {
        String descricaoNormalizada = NormalizadorDescricao.normalizar(item.descricao());
        if (item.modalidadePreco() == null || descricaoNormalizada == null) {
            return null;
        }
        String chaveItem = item.referencia() != null ? item.referencia() : descricaoNormalizada;
        String chaveDaLeitura = chaveItem + "|" + item.unidade() + "|" + item.modalidadePreco();
        return apresentacoesDaLeitura.computeIfAbsent(
                chaveDaLeitura,
                chave -> apresentacoes
                        .findByFornecedorIdAndChaveItemAndUnidadeAndModalidadePreco(
                                fornecedorId, chaveItem, item.unidade(), item.modalidadePreco())
                        .orElseGet(() -> apresentacoes.saveAndFlush(ApresentacaoFornecedor.nova(
                                fornecedorId,
                                item.referencia(),
                                item.descricao().strip(),
                                descricaoNormalizada,
                                item.unidade(),
                                item.modalidadePreco(),
                                agora)))
                        .getId());
    }
}
