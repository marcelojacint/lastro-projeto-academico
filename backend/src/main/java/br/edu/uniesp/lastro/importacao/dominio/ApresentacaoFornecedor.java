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
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "apresentacao_fornecedor")
public class ApresentacaoFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long fornecedorId;

    @Column(updatable = false)
    private String referencia;

    @Column(nullable = false)
    private String descricaoOriginal;

    @Column(nullable = false, updatable = false)
    private String descricaoNormalizada;

    @Column(updatable = false)
    private String unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ModalidadePreco modalidadePreco;

    @Generated
    @Column(insertable = false, updatable = false)
    private String chaveItem;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected ApresentacaoFornecedor() {}

    public static ApresentacaoFornecedor nova(
            Long fornecedorId,
            String referencia,
            String descricaoOriginal,
            String descricaoNormalizada,
            String unidade,
            ModalidadePreco modalidadePreco,
            Instant agora) {
        ApresentacaoFornecedor apresentacao = new ApresentacaoFornecedor();
        apresentacao.fornecedorId = fornecedorId;
        apresentacao.referencia = referencia;
        apresentacao.descricaoOriginal = descricaoOriginal;
        apresentacao.descricaoNormalizada = descricaoNormalizada;
        apresentacao.unidade = unidade;
        apresentacao.modalidadePreco = modalidadePreco;
        apresentacao.criadaEm = agora.truncatedTo(ChronoUnit.MICROS);
        return apresentacao;
    }

    public Long getId() {
        return id;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getDescricaoOriginal() {
        return descricaoOriginal;
    }

    public String getDescricaoNormalizada() {
        return descricaoNormalizada;
    }

    public String getUnidade() {
        return unidade;
    }

    public ModalidadePreco getModalidadePreco() {
        return modalidadePreco;
    }

    public String getChaveItem() {
        return chaveItem;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
