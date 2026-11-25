package br.edu.uniesp.lastro.fornecedor.api;

import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.time.Instant;

public record FornecedorResposta(
        Long id,
        String nome,
        String cnpj,
        String contato,
        LayoutPdf layoutPdf,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    static FornecedorResposta de(Fornecedor fornecedor) {
        return new FornecedorResposta(
                fornecedor.getId(),
                fornecedor.getNome(),
                fornecedor.getCnpj(),
                fornecedor.getContato(),
                fornecedor.getLayoutPdf(),
                fornecedor.isAtivo(),
                fornecedor.getCriadoEm(),
                fornecedor.getAtualizadoEm());
    }
}
