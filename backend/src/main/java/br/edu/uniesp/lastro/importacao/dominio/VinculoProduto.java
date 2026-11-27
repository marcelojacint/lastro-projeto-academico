package br.edu.uniesp.lastro.importacao.dominio;

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
@Table(name = "vinculo_produto")
public class VinculoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long apresentacaoId;

    @Column(nullable = false, updatable = false)
    private Long produtoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrigemVinculo origem;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false, updatable = false)
    private Instant confirmadoEm;

    private Instant desativadoEm;

    protected VinculoProduto() {}

    public static VinculoProduto automatico(Long apresentacaoId, Long produtoId, Instant agora) {
        return novo(apresentacaoId, produtoId, OrigemVinculo.AUTOMATICA, agora);
    }

    public static VinculoProduto manual(Long apresentacaoId, Long produtoId, Instant agora) {
        return novo(apresentacaoId, produtoId, OrigemVinculo.MANUAL, agora);
    }

    public void desativar(Instant agora) {
        this.ativo = false;
        this.desativadoEm = agora.truncatedTo(ChronoUnit.MICROS);
    }

    private static VinculoProduto novo(Long apresentacaoId, Long produtoId, OrigemVinculo origem, Instant agora) {
        VinculoProduto vinculo = new VinculoProduto();
        vinculo.apresentacaoId = apresentacaoId;
        vinculo.produtoId = produtoId;
        vinculo.origem = origem;
        vinculo.ativo = true;
        vinculo.confirmadoEm = agora.truncatedTo(ChronoUnit.MICROS);
        return vinculo;
    }

    public Long getId() {
        return id;
    }

    public Long getApresentacaoId() {
        return apresentacaoId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public OrigemVinculo getOrigem() {
        return origem;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getConfirmadoEm() {
        return confirmadoEm;
    }

    public Instant getDesativadoEm() {
        return desativadoEm;
    }
}
