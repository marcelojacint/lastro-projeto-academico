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
@Table(name = "ausencia_item")
public class AusenciaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID importacaoId;

    @Column(nullable = false)
    private Long apresentacaoId;

    @Column(nullable = false)
    private BigDecimal ultimoPrecoValido;

    @Column(nullable = false)
    private Instant registradaEm;

    protected AusenciaItem() {}

    public static AusenciaItem registrar(
            UUID importacaoId, Long apresentacaoId, BigDecimal ultimoPrecoValido, Instant agora) {
        if (ultimoPrecoValido == null || ultimoPrecoValido.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Ausência exige o último preço válido da apresentação " + apresentacaoId);
        }
        AusenciaItem ausencia = new AusenciaItem();
        ausencia.importacaoId = importacaoId;
        ausencia.apresentacaoId = apresentacaoId;
        ausencia.ultimoPrecoValido = ultimoPrecoValido;
        ausencia.registradaEm = agora.truncatedTo(ChronoUnit.MICROS);
        return ausencia;
    }

    public Long getId() {
        return id;
    }

    public UUID getImportacaoId() {
        return importacaoId;
    }

    public Long getApresentacaoId() {
        return apresentacaoId;
    }

    public BigDecimal getUltimoPrecoValido() {
        return ultimoPrecoValido;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}
