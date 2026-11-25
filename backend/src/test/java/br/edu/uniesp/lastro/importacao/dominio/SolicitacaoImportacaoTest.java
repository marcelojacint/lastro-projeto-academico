package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import br.edu.uniesp.lastro.fornecedor.dominio.DadosFornecedor;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SolicitacaoImportacaoTest {

    private static final byte[] PDF = "%PDF-1.4\nteste lastro\n".getBytes(StandardCharsets.US_ASCII);
    private static final Instant AGORA = Instant.parse("2026-11-25T10:00:00Z");

    @Test
    void deveAceitarPdfECalcularTamanhoEHashSha256() {
        SolicitacaoImportacao solicitacao = SolicitacaoImportacao.validar(7L, "Fornecedor_1.pdf", PDF);

        assertThat(solicitacao.fornecedorId()).isEqualTo(7L);
        assertThat(solicitacao.nomeArquivo()).isEqualTo("Fornecedor_1.pdf");
        assertThat(solicitacao.tamanhoBytes()).isEqualTo(22);
        assertThat(solicitacao.hashSha256())
                .isEqualTo("0797348fa158c4cedd656ee54723e548f6ec5a4d00c90542b962ff6b3622c7ee");
    }

    @Test
    void deveRecusarArquivoSemAssinaturaDePdfMesmoComExtensaoPdf() {
        byte[] texto = "ref;produto;preco".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> SolicitacaoImportacao.validar(7L, "tabela.pdf", texto))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .containsExactly(new ErroCampo("arquivo", "O arquivo deve ser um PDF")));
    }

    @Test
    void deveRecusarPdfAcimaDeCincoMegabytes() {
        byte[] grande = Arrays.copyOf(PDF, (int) SolicitacaoImportacao.TAMANHO_MAXIMO_BYTES + 1);

        assertThatThrownBy(() -> SolicitacaoImportacao.validar(7L, "grande.pdf", grande))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .containsExactly(new ErroCampo("arquivo", "O arquivo deve ter no máximo 5 MB")));
    }

    @Test
    void deveAceitarPdfComExatamenteCincoMegabytes() {
        byte[] limite = Arrays.copyOf(PDF, (int) SolicitacaoImportacao.TAMANHO_MAXIMO_BYTES);

        assertThat(SolicitacaoImportacao.validar(7L, "limite.pdf", limite).tamanhoBytes())
                .isEqualTo(SolicitacaoImportacao.TAMANHO_MAXIMO_BYTES);
    }

    @Test
    void deveJuntarTodosOsErrosQuandoFaltamFornecedorEArquivo() {
        assertThatThrownBy(() -> SolicitacaoImportacao.validar(null, null, new byte[0]))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .containsExactly(
                                        new ErroCampo("fornecedorId", "Fornecedor é obrigatório"),
                                        new ErroCampo("arquivo", "Arquivo é obrigatório")));
    }

    @Test
    void deveGuardarSoONomeDoArquivoSemOCaminho() {
        assertThat(SolicitacaoImportacao.validar(7L, "C:\\Users\\loja\\tabela.pdf", PDF)
                        .nomeArquivo())
                .isEqualTo("tabela.pdf");
        assertThat(SolicitacaoImportacao.validar(7L, "/home/loja/tabela.pdf", PDF)
                        .nomeArquivo())
                .isEqualTo("tabela.pdf");
        assertThat(SolicitacaoImportacao.validar(7L, "  ", PDF).nomeArquivo()).isEqualTo("arquivo.pdf");
    }

    @Test
    void deveRecusarFornecedorInativo() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dadosFornecedor(true), AGORA);
        fornecedor.atualizar(dadosFornecedor(false), AGORA);
        SolicitacaoImportacao solicitacao = SolicitacaoImportacao.validar(7L, "tabela.pdf", PDF);

        assertThatThrownBy(() -> solicitacao.exigirFornecedorAtivo(fornecedor))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .containsExactly(new ErroCampo(
                                        "fornecedorId", "Fornecedor inativo não pode receber importações")));
    }

    @Test
    void deveAceitarFornecedorAtivo() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dadosFornecedor(true), AGORA);
        SolicitacaoImportacao solicitacao = SolicitacaoImportacao.validar(7L, "tabela.pdf", PDF);

        assertThatCode(() -> solicitacao.exigirFornecedorAtivo(fornecedor)).doesNotThrowAnyException();
    }

    private static DadosFornecedor dadosFornecedor(boolean ativo) {
        return new DadosFornecedor("Natural Sul", null, null, "TABELA_REFERENCIA", ativo);
    }
}
