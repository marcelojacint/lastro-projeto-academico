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
import java.util.UUID;

@Entity
@Table(name = "importacao")
public class Importacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Long fornecedorId;

    @Column(nullable = false, updatable = false)
    private String nomeArquivo;

    @Column(nullable = false, updatable = false)
    private String hashSha256;

    @Column(nullable = false, updatable = false)
    private long tamanhoBytes;

    @Column(nullable = false, updatable = false)
    private Instant recebidaEm;

    private Instant concluidaEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusImportacao status;

    @Column(nullable = false)
    private int totalItens;

    @Column(nullable = false)
    private int itensValidos;

    @Column(nullable = false)
    private int itensPendentes;

    @Enumerated(EnumType.STRING)
    private MotivoFalha motivoFalha;

    protected Importacao() {}

    public static Importacao receber(
            Long fornecedorId, String nomeArquivo, String hashSha256, long tamanhoBytes, Instant agora) {
        Importacao importacao = new Importacao();
        importacao.fornecedorId = fornecedorId;
        importacao.nomeArquivo = nomeArquivo;
        importacao.hashSha256 = hashSha256;
        importacao.tamanhoBytes = tamanhoBytes;
        importacao.recebidaEm = agora.truncatedTo(ChronoUnit.MICROS);
        importacao.status = StatusImportacao.RECEBIDA;
        return importacao;
    }

    public void iniciarConferencia(int totalItens, int itensValidos) {
        this.status = StatusImportacao.EM_CONFERENCIA;
        this.totalItens = totalItens;
        this.itensValidos = itensValidos;
        this.itensPendentes = totalItens - itensValidos;
        this.motivoFalha = null;
    }

    public void falhar(MotivoFalha motivo) {
        this.status = StatusImportacao.FALHOU;
        this.motivoFalha = motivo;
    }

    public UUID getId() {
        return id;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public Instant getRecebidaEm() {
        return recebidaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    public StatusImportacao getStatus() {
        return status;
    }

    public int getTotalItens() {
        return totalItens;
    }

    public int getItensValidos() {
        return itensValidos;
    }

    public int getItensPendentes() {
        return itensPendentes;
    }

    public MotivoFalha getMotivoFalha() {
        return motivoFalha;
    }
}
