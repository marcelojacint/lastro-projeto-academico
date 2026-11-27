package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.importacao.dominio.ApresentacaoFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.ItemExtraido;
import br.edu.uniesp.lastro.importacao.dominio.NormalizadorDescricao;
import br.edu.uniesp.lastro.importacao.infraestrutura.ApresentacaoFornecedorRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ResolucaoApresentacao {

    private final ApresentacaoFornecedorRepository apresentacoes;

    public ResolucaoApresentacao(ApresentacaoFornecedorRepository apresentacoes) {
        this.apresentacoes = apresentacoes;
    }

    public Long resolver(Long fornecedorId, ItemExtraido item, Instant agora) {
        String descricaoNormalizada = NormalizadorDescricao.normalizar(item.descricao());
        if (item.modalidadePreco() == null || descricaoNormalizada == null) {
            return null;
        }
        String chaveItem = item.referencia() != null ? item.referencia() : descricaoNormalizada;
        return apresentacoes
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
                .getId();
    }
}
