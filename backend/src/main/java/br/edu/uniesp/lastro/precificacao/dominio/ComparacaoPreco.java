package br.edu.uniesp.lastro.precificacao.dominio;

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
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "comparacao_preco")
public class ComparacaoPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID importacaoId;

    @Column(nullable = false)
    private Long itemImportadoId;

    private BigDecimal precoAnterior;

    private BigDecimal precoAtual;

    private BigDecimal diferenca;

    private BigDecimal variacaoPercentual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classificacao classificacao;

    @Column(nullable = false)
    private Instant comparadaEm;

    protected ComparacaoPreco() {}

    public static ComparacaoPreco registrar(
            UUID importacaoId, Long itemImportadoId, ResultadoComparacao resultado, Instant agora) {
        ComparacaoPreco comparacao = new ComparacaoPreco();
        comparacao.importacaoId = importacaoId;
        comparacao.itemImportadoId = itemImportadoId;
        comparacao.precoAnterior = resultado.precoAnterior();
        comparacao.precoAtual = resultado.precoAtual();
        comparacao.diferenca = resultado.diferenca();
        comparacao.variacaoPercentual = resultado.variacaoPercentual();
        comparacao.classificacao = resultado.classificacao();
        comparacao.comparadaEm = agora.truncatedTo(ChronoUnit.MICROS);
        return comparacao;
    }

    public Long getId() {
        return id;
    }

    public UUID getImportacaoId() {
        return importacaoId;
    }

    public Long getItemImportadoId() {
        return itemImportadoId;
    }

    public BigDecimal getPrecoAnterior() {
        return precoAnterior;
    }

    public BigDecimal getPrecoAtual() {
        return precoAtual;
    }

    public BigDecimal getDiferenca() {
        return diferenca;
    }

    public BigDecimal getVariacaoPercentual() {
        return variacaoPercentual;
    }

    public Classificacao getClassificacao() {
        return classificacao;
    }

    public Instant getComparadaEm() {
        return comparadaEm;
    }
}
