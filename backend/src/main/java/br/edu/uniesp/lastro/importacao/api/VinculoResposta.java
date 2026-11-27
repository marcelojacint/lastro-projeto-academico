package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.importacao.aplicacao.VinculoService.VinculoComProduto;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.OrigemVinculo;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoAtivo;
import java.time.Instant;

public record VinculoResposta(
        Long id,
        Long apresentacaoId,
        String referencia,
        String descricao,
        String unidade,
        ModalidadePreco modalidadePreco,
        Long produtoId,
        String produtoNome,
        OrigemVinculo origem,
        Instant confirmadoEm) {

    static VinculoResposta de(VinculoComProduto resultado) {
        VinculoAtivo vinculo = resultado.vinculo();
        return new VinculoResposta(
                vinculo.id(),
                vinculo.apresentacaoId(),
                vinculo.referencia(),
                vinculo.descricao(),
                vinculo.unidade(),
                vinculo.modalidadePreco(),
                vinculo.produtoId(),
                resultado.produtoNome(),
                vinculo.origem(),
                vinculo.confirmadoEm());
    }
}
