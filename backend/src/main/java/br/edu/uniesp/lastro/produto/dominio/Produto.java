package br.edu.uniesp.lastro.produto.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;

    @Column(nullable = false)
    private String nome;

    private String categoria;

    private String unidade;

    private BigDecimal precoCompra;

    @Column(nullable = false)
    private BigDecimal precoVenda;

    @Column(nullable = false)
    private BigDecimal estoqueAtual;

    @Column(nullable = false)
    private BigDecimal estoqueMinimo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusProduto status;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Produto() {}

    public static Produto cadastrar(DadosProduto dados, Instant agora) {
        Produto produto = new Produto();
        produto.aplicar(dados, StatusProduto.ATIVO);
        produto.criadoEm = agora.truncatedTo(ChronoUnit.MICROS);
        produto.atualizadoEm = produto.criadoEm;
        return produto;
    }

    public void atualizar(DadosProduto dados, Instant agora) {
        aplicar(dados, status);
        atualizadoEm = agora.truncatedTo(ChronoUnit.MICROS);
    }

    private void aplicar(DadosProduto dados, StatusProduto statusPadrao) {
        ValidacaoProduto validacao = new ValidacaoProduto();
        String novoCodigo =
                validacao.textoOpcional("codigo", "Código", dados.codigo(), ValidacaoProduto.TAMANHO_MAXIMO_CODIGO);
        String novoNome =
                validacao.textoObrigatorio("nome", "Nome", dados.nome(), ValidacaoProduto.TAMANHO_MAXIMO_NOME);
        String novaCategoria = validacao.textoOpcional(
                "categoria", "Categoria", dados.categoria(), ValidacaoProduto.TAMANHO_MAXIMO_CATEGORIA);
        String novaUnidade =
                validacao.textoOpcional("unidade", "Unidade", dados.unidade(), ValidacaoProduto.TAMANHO_MAXIMO_UNIDADE);
        BigDecimal novoPrecoCompra = validacao.precoOpcional("precoCompra", "Preço de compra", dados.precoCompra());
        BigDecimal novoPrecoVenda = validacao.precoObrigatorio("precoVenda", "Preço de venda", dados.precoVenda());
        BigDecimal novoEstoqueAtual = validacao.estoque("estoqueAtual", "Estoque atual", dados.estoqueAtual());
        BigDecimal novoEstoqueMinimo = validacao.estoque("estoqueMinimo", "Estoque mínimo", dados.estoqueMinimo());
        StatusProduto novoStatus = validacao.status(dados.status(), statusPadrao);
        validacao.lancarSeHouverErros();

        codigo = novoCodigo;
        nome = novoNome;
        categoria = novaCategoria;
        unidade = novaUnidade;
        precoCompra = novoPrecoCompra;
        precoVenda = novoPrecoVenda;
        estoqueAtual = novoEstoqueAtual;
        estoqueMinimo = novoEstoqueMinimo;
        status = novoStatus;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getUnidade() {
        return unidade;
    }

    public BigDecimal getPrecoCompra() {
        return precoCompra;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public BigDecimal getEstoqueAtual() {
        return estoqueAtual;
    }

    public BigDecimal getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public StatusProduto getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
