package br.edu.uniesp.lastro.fornecedor.api;

import br.edu.uniesp.lastro.fornecedor.dominio.DadosFornecedor;

public record FornecedorRequisicao(String nome, String cnpj, String contato, String layoutPdf, Boolean ativo) {

    DadosFornecedor paraDados() {
        return new DadosFornecedor(nome, cnpj, contato, layoutPdf, ativo);
    }
}
