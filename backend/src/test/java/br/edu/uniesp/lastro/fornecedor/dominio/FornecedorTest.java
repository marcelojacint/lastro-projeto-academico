package br.edu.uniesp.lastro.fornecedor.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FornecedorTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T10:00:00Z");

    @Test
    void deveNascerAtivoMesmoQuandoCadastroPedeInativo() {
        Fornecedor fornecedor = Fornecedor.cadastrar(
                new DadosFornecedor(" Natural Sul ", null, " (83) 99999-0000 ", "tabela_referencia", false), AGORA);

        assertThat(fornecedor.getNome()).isEqualTo("Natural Sul");
        assertThat(fornecedor.getContato()).isEqualTo("(83) 99999-0000");
        assertThat(fornecedor.getLayoutPdf()).isEqualTo(LayoutPdf.TABELA_REFERENCIA);
        assertThat(fornecedor.isAtivo()).isTrue();
        assertThat(fornecedor.getCriadoEm()).isEqualTo(AGORA);
        assertThat(fornecedor.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveGuardarCnpjSoComDigitos() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dados("12.345.678/0001-90", "ATACADO_VAREJO"), AGORA);

        assertThat(fornecedor.getCnpj()).isEqualTo("12345678000190");
    }

    @Test
    void deveTratarCnpjEmBrancoComoNulo() {
        DadosFornecedor dados = dados("   ", "ATACADO_VAREJO");

        Fornecedor fornecedor = Fornecedor.cadastrar(dados, AGORA);

        assertThat(fornecedor.getCnpj()).isNull();
        assertThat(dados.cnpjInformado()).isNull();
    }

    @Test
    void deveRecusarCnpjSemQuatorzeDigitos() {
        assertThatThrownBy(() -> Fornecedor.cadastrar(dados("12.345.678/0001-9", "ATACADO_VAREJO"), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .containsExactly(new ErroCampo("cnpj", "CNPJ deve ter 14 dígitos")));
    }

    @Test
    void deveRecusarCnpjComLetras() {
        assertThatThrownBy(() -> Fornecedor.cadastrar(dados("12345678000190A", "ATACADO_VAREJO"), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("cnpj"));
    }

    @Test
    void deveExigirNomeELayout() {
        assertThatThrownBy(() -> Fornecedor.cadastrar(new DadosFornecedor(" ", null, null, null, null), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("nome", "layoutPdf"));
    }

    @Test
    void deveRecusarLayoutDesconhecido() {
        assertThatThrownBy(() -> Fornecedor.cadastrar(dados(null, "PLANILHA"), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("layoutPdf"));
    }

    @Test
    void deveRecusarNomeMaiorQueOPermitido() {
        String nomeLongo = "a".repeat(ValidacaoFornecedor.TAMANHO_MAXIMO_NOME + 1);

        assertThatThrownBy(() ->
                        Fornecedor.cadastrar(new DadosFornecedor(nomeLongo, null, null, "ATACADO_VAREJO", null), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("nome"));
    }

    @Test
    void deveEditarEInativarMantendoDataDeCriacao() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dados("12345678000190", "TABELA_REFERENCIA"), AGORA);
        Instant depois = AGORA.plusSeconds(60);

        fornecedor.atualizar(new DadosFornecedor("Novo nome", "", "Maria", "ATACADO_VAREJO", false), depois);

        assertThat(fornecedor.getNome()).isEqualTo("Novo nome");
        assertThat(fornecedor.getCnpj()).isNull();
        assertThat(fornecedor.getContato()).isEqualTo("Maria");
        assertThat(fornecedor.getLayoutPdf()).isEqualTo(LayoutPdf.ATACADO_VAREJO);
        assertThat(fornecedor.isAtivo()).isFalse();
        assertThat(fornecedor.getCriadoEm()).isEqualTo(AGORA);
        assertThat(fornecedor.getAtualizadoEm()).isEqualTo(depois);
    }

    @Test
    void deveReativarFornecedorInativo() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dados(null, "TABELA_REFERENCIA"), AGORA);
        fornecedor.atualizar(new DadosFornecedor("Natural Sul", null, null, "TABELA_REFERENCIA", false), AGORA);

        fornecedor.atualizar(new DadosFornecedor("Natural Sul", null, null, "TABELA_REFERENCIA", true), AGORA);

        assertThat(fornecedor.isAtivo()).isTrue();
    }

    @Test
    void deveManterSituacaoAtualQuandoEdicaoNaoInformaAtivo() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dados(null, "TABELA_REFERENCIA"), AGORA);
        fornecedor.atualizar(new DadosFornecedor("Natural Sul", null, null, "TABELA_REFERENCIA", false), AGORA);

        fornecedor.atualizar(dados(null, "TABELA_REFERENCIA"), AGORA.plusSeconds(1));

        assertThat(fornecedor.isAtivo()).isFalse();
    }

    @Test
    void naoDeveAlterarNadaQuandoEdicaoForInvalida() {
        Fornecedor fornecedor = Fornecedor.cadastrar(dados("12345678000190", "TABELA_REFERENCIA"), AGORA);

        assertThatThrownBy(() ->
                        fornecedor.atualizar(new DadosFornecedor("", "123", null, null, false), AGORA.plusSeconds(1)))
                .isInstanceOf(DadosInvalidosException.class);

        assertThat(fornecedor.getNome()).isEqualTo("Natural Sul");
        assertThat(fornecedor.getCnpj()).isEqualTo("12345678000190");
        assertThat(fornecedor.getLayoutPdf()).isEqualTo(LayoutPdf.TABELA_REFERENCIA);
        assertThat(fornecedor.isAtivo()).isTrue();
        assertThat(fornecedor.getAtualizadoEm()).isEqualTo(AGORA);
    }

    private static DadosFornecedor dados(String cnpj, String layoutPdf) {
        return new DadosFornecedor("Natural Sul", cnpj, null, layoutPdf, null);
    }
}
