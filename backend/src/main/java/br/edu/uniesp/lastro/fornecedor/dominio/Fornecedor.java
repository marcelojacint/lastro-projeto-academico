package br.edu.uniesp.lastro.fornecedor.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "fornecedor")
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String cnpj;

    private String contato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LayoutPdf layoutPdf;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Fornecedor() {}

    public static Fornecedor cadastrar(DadosFornecedor dados, Instant agora) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.aplicar(dados, true);
        fornecedor.criadoEm = agora.truncatedTo(ChronoUnit.MICROS);
        fornecedor.atualizadoEm = fornecedor.criadoEm;
        return fornecedor;
    }

    public void atualizar(DadosFornecedor dados, Instant agora) {
        aplicar(dados, dados.ativo() == null ? ativo : dados.ativo());
        atualizadoEm = agora.truncatedTo(ChronoUnit.MICROS);
    }

    private void aplicar(DadosFornecedor dados, boolean novoAtivo) {
        ValidacaoFornecedor validacao = new ValidacaoFornecedor();
        String novoNome = validacao.nome(dados.nome());
        String novoCnpj = validacao.cnpj(dados);
        String novoContato = validacao.contato(dados.contato());
        LayoutPdf novoLayout = validacao.layoutPdf(dados.layoutPdf());
        validacao.lancarSeHouverErros();

        nome = novoNome;
        cnpj = novoCnpj;
        contato = novoContato;
        layoutPdf = novoLayout;
        ativo = novoAtivo;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getContato() {
        return contato;
    }

    public LayoutPdf getLayoutPdf() {
        return layoutPdf;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
