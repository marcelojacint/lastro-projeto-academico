package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.importacao.aplicacao.ImportacaoComFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.MotivoFalha;
import br.edu.uniesp.lastro.importacao.dominio.StatusImportacao;
import java.time.Instant;
import java.util.UUID;

public record ImportacaoResposta(
        UUID id,
        Long fornecedorId,
        String fornecedorNome,
        String nomeArquivo,
        long tamanhoBytes,
        String hashSha256,
        Instant recebidaEm,
        Instant concluidaEm,
        StatusImportacao status,
        int totalItens,
        int itensValidos,
        int itensPendentes,
        MotivoFalha motivoFalha) {

    public static ImportacaoResposta de(ImportacaoComFornecedor resultado) {
        Importacao importacao = resultado.importacao();
        return new ImportacaoResposta(
                importacao.getId(),
                importacao.getFornecedorId(),
                resultado.fornecedorNome(),
                importacao.getNomeArquivo(),
                importacao.getTamanhoBytes(),
                importacao.getHashSha256(),
                importacao.getRecebidaEm(),
                importacao.getConcluidaEm(),
                importacao.getStatus(),
                importacao.getTotalItens(),
                importacao.getItensValidos(),
                importacao.getItensPendentes(),
                importacao.getMotivoFalha());
    }
}
