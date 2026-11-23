package br.edu.uniesp.lastro.produto.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProdutoTest {

    private static final Instant AGORA = Instant.parse("2026-11-19T10:00:00Z");

    @Test
    void deveCadastrarComStatusAtivoEEstoquesZeradosQuandoNaoInformados() {
        Produto produto = Produto.cadastrar(dados(" Granola ", "12.50", null, null, null), AGORA);

        assertThat(produto.getNome()).isEqualTo("Granola");
        assertThat(produto.getStatus()).isEqualTo(StatusProduto.ATIVO);
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo("0");
        assertThat(produto.getEstoqueMinimo()).isEqualByComparingTo("0");
        assertThat(produto.getCriadoEm()).isEqualTo(AGORA);
        assertThat(produto.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveTratarCodigoEmBrancoComoNaoInformado() {
        DadosProduto dados =
                new DadosProduto("   ", "Granola", null, null, null, new BigDecimal("10"), null, null, null);

        Produto produto = Produto.cadastrar(dados, AGORA);

        assertThat(produto.getCodigo()).isNull();
        assertThat(dados.codigoInformado()).isNull();
    }

    @Test
    void deveExigirNomeEPrecoDeVenda() {
        assertThatThrownBy(() -> Produto.cadastrar(dados("  ", null, null, null, null), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("nome", "precoVenda"));
    }

    @Test
    void deveRecusarPrecosEEstoquesNegativosListandoTodosOsErros() {
        DadosProduto dados = new DadosProduto(
                null,
                "Granola",
                null,
                null,
                new BigDecimal("-1"),
                new BigDecimal("-0.01"),
                new BigDecimal("-2"),
                new BigDecimal("-3"),
                null);

        assertThatThrownBy(() -> Produto.cadastrar(dados, AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("precoCompra", "precoVenda", "estoqueAtual", "estoqueMinimo"));
    }

    @Test
    void deveAceitarPrecoDeVendaZero() {
        Produto produto = Produto.cadastrar(dados("Amostra", "0", null, null, null), AGORA);

        assertThat(produto.getPrecoVenda()).isEqualByComparingTo("0");
    }

    @Test
    void deveRecusarPrecoComMaisDeDuasCasasDecimais() {
        assertThatThrownBy(() -> Produto.cadastrar(dados("Granola", "10.555", null, null, null), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::mensagem)
                                .containsExactly("Preço de venda deve ter no máximo 2 casas decimais"));
    }

    @Test
    void deveRecusarNomeMaiorQueOPermitido() {
        String nomeLongo = "a".repeat(ValidacaoProduto.TAMANHO_MAXIMO_NOME + 1);

        assertThatThrownBy(() -> Produto.cadastrar(dados(nomeLongo, "10", null, null, null), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("nome"));
    }

    @Test
    void deveRecusarStatusDesconhecido() {
        assertThatThrownBy(() -> Produto.cadastrar(dados("Granola", "10", null, null, "VENDIDO"), AGORA))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        excecao -> assertThat(excecao.getErros())
                                .extracting(ErroCampo::campo)
                                .containsExactly("status"));
    }

    @Test
    void deveAtualizarDadosEDataDeAtualizacaoMantendoDataDeCriacao() {
        Produto produto = Produto.cadastrar(dados("Granola", "10", null, null, null), AGORA);
        Instant depois = AGORA.plusSeconds(60);

        produto.atualizar(dados("Granola tradicional", "11.90", "5", "2", "INATIVO"), depois);

        assertThat(produto.getNome()).isEqualTo("Granola tradicional");
        assertThat(produto.getPrecoVenda()).isEqualByComparingTo("11.90");
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo("5");
        assertThat(produto.getEstoqueMinimo()).isEqualByComparingTo("2");
        assertThat(produto.getStatus()).isEqualTo(StatusProduto.INATIVO);
        assertThat(produto.getCriadoEm()).isEqualTo(AGORA);
        assertThat(produto.getAtualizadoEm()).isEqualTo(depois);
    }

    @Test
    void deveManterStatusAtualQuandoEdicaoNaoInformaStatus() {
        Produto produto = Produto.cadastrar(dados("Granola", "10", null, null, "INATIVO"), AGORA);

        produto.atualizar(dados("Granola", "12", null, null, null), AGORA.plusSeconds(1));

        assertThat(produto.getStatus()).isEqualTo(StatusProduto.INATIVO);
    }

    @Test
    void naoDeveAlterarNadaQuandoEdicaoForInvalida() {
        Produto produto = Produto.cadastrar(dados("Granola", "10", null, null, null), AGORA);

        assertThatThrownBy(() -> produto.atualizar(dados("", "12", null, null, null), AGORA.plusSeconds(1)))
                .isInstanceOf(DadosInvalidosException.class);

        assertThat(produto.getNome()).isEqualTo("Granola");
        assertThat(produto.getPrecoVenda()).isEqualByComparingTo("10");
        assertThat(produto.getAtualizadoEm()).isEqualTo(AGORA);
    }

    private static DadosProduto dados(
            String nome, String precoVenda, String estoqueAtual, String estoqueMinimo, String status) {
        return new DadosProduto(
                null,
                nome,
                "Cereais",
                "UN",
                null,
                numero(precoVenda),
                numero(estoqueAtual),
                numero(estoqueMinimo),
                status);
    }

    private static BigDecimal numero(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
