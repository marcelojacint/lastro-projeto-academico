package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "item_importado")
public class ItemImportado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private UUID importacaoId;

    @Column(nullable = false, updatable = false)
    private int numeroLinha;

    @Column(nullable = false, updatable = false)
    private String linhaOriginal;

    private String referencia;

    private String descricao;

    private String unidade;

    @Enumerated(EnumType.STRING)
    private ModalidadePreco modalidadePreco;

    private BigDecimal quantidade;

    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoItem situacao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<MensagemErro> erros = new ArrayList<>();

    private Long apresentacaoId;

    protected ItemImportado() {}

    public static ItemImportado registrar(
            UUID importacaoId,
            ItemExtraido item,
            SituacaoItem situacao,
            List<MensagemErro> erros,
            Long apresentacaoId) {
        ItemImportado importado = new ItemImportado();
        importado.importacaoId = importacaoId;
        importado.numeroLinha = item.numeroLinha();
        importado.linhaOriginal = item.linhaOriginal();
        importado.referencia = item.referencia();
        importado.descricao = item.descricao();
        importado.unidade = item.unidade();
        importado.modalidadePreco = item.modalidadePreco();
        importado.quantidade = item.quantidade();
        importado.preco = item.preco();
        importado.situacao = situacao;
        importado.erros = new ArrayList<>(erros);
        importado.apresentacaoId = apresentacaoId;
        return importado;
    }

    public static ItemImportado registrarValidando(
            UUID importacaoId,
            ItemExtraido item,
            LayoutPdf layout,
            Long apresentacaoId,
            boolean apresentacaoAssociada) {
        ResultadoValidacaoItem resultado = ValidacaoItem.validar(item, layout);
        return registrar(
                importacaoId,
                item,
                resultado.situacaoComAssociacao(apresentacaoAssociada),
                resultado.erros(),
                apresentacaoId);
    }

    public void corrigir(ItemExtraido corrigido, LayoutPdf layout, Long apresentacaoId, boolean apresentacaoAssociada) {
        ResultadoValidacaoItem resultado = ValidacaoItem.validar(corrigido, layout);
        this.referencia = corrigido.referencia();
        this.descricao = corrigido.descricao();
        this.unidade = corrigido.unidade();
        this.preco = corrigido.preco();
        this.situacao = resultado.situacaoComAssociacao(apresentacaoAssociada);
        this.erros = new ArrayList<>(resultado.erros());
        this.apresentacaoId = apresentacaoId;
    }

    public Long getId() {
        return id;
    }

    public UUID getImportacaoId() {
        return importacaoId;
    }

    public int getNumeroLinha() {
        return numeroLinha;
    }

    public String getLinhaOriginal() {
        return linhaOriginal;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public ModalidadePreco getModalidadePreco() {
        return modalidadePreco;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public SituacaoItem getSituacao() {
        return situacao;
    }

    public List<MensagemErro> getErros() {
        return List.copyOf(erros);
    }

    public Long getApresentacaoId() {
        return apresentacaoId;
    }
}
