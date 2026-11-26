package br.edu.uniesp.lastro.precificacao.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "historico_preco")
public class HistoricoPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long apresentacaoId;

    @Column(nullable = false)
    private UUID importacaoId;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(nullable = false)
    private Instant registradoEm;

    protected HistoricoPreco() {}

    public static HistoricoPreco registrar(Long apresentacaoId, UUID importacaoId, BigDecimal preco, Instant agora) {
        HistoricoPreco historico = new HistoricoPreco();
        historico.apresentacaoId = apresentacaoId;
        historico.importacaoId = importacaoId;
        historico.preco = preco;
        historico.registradoEm = agora.truncatedTo(ChronoUnit.MICROS);
        return historico;
    }

    public Long getId() {
        return id;
    }

    public Long getApresentacaoId() {
        return apresentacaoId;
    }

    public UUID getImportacaoId() {
        return importacaoId;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
