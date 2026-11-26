package br.edu.uniesp.lastro.importacao.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.DadosFornecedor;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.fornecedor.infraestrutura.FornecedorRepository;
import br.edu.uniesp.lastro.importacao.dominio.ApresentacaoFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemExtraido;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.dominio.StatusImportacao;
import br.edu.uniesp.lastro.precificacao.dominio.HistoricoPreco;
import br.edu.uniesp.lastro.precificacao.infraestrutura.HistoricoPrecoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ContratoImportacaoPersistenciaTest {

    private static final Instant AGORA = Instant.parse("2026-11-25T10:00:00Z");

    @Autowired
    private FornecedorRepository fornecedores;

    @Autowired
    private ImportacaoRepository importacoes;

    @Autowired
    private ApresentacaoFornecedorRepository apresentacoes;

    @Autowired
    private ItemImportadoRepository itens;

    @Autowired
    private HistoricoPrecoRepository historicos;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveGravarImportacaoComIdUuidEStatusRecebida() {
        Importacao importacao = importacoes.saveAndFlush(importacao(fornecedor(), hashUnico()));

        assertThat(importacao.getId()).isNotNull();
        assertThat(importacoes.findById(importacao.getId()))
                .get()
                .extracting(Importacao::getStatus, Importacao::getTotalItens)
                .containsExactly(StatusImportacao.RECEBIDA, 0);
    }

    @Test
    void deveBloquearMesmoHashDoMesmoFornecedorEnquantoImportacaoValer() {
        Long fornecedorId = fornecedor();
        String hash = hashUnico();
        importacoes.saveAndFlush(importacao(fornecedorId, hash));

        assertThatThrownBy(() -> importacoes.saveAndFlush(importacao(fornecedorId, hash)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("uk_importacao_fornecedor_hash");
    }

    @Test
    void devePermitirMesmoHashDepoisDeImportacaoDescartadaOuQueFalhou() {
        Long fornecedorId = fornecedor();
        String hash = hashUnico();
        Importacao descartada = importacoes.saveAndFlush(importacao(fornecedorId, hash));
        mudarStatus(descartada, "DESCARTADA", null);
        Importacao falhou = importacoes.saveAndFlush(importacao(fornecedorId, hash));
        mudarStatus(falhou, "FALHOU", "LAYOUT_NAO_RECONHECIDO");

        Importacao reenvio = importacoes.saveAndFlush(importacao(fornecedorId, hash));

        assertThat(reenvio.getId()).isNotIn(descartada.getId(), falhou.getId());
    }

    @Test
    void devePermitirMesmoHashEmOutroFornecedor() {
        String hash = hashUnico();
        importacoes.saveAndFlush(importacao(fornecedor(), hash));

        Importacao outra = importacoes.saveAndFlush(importacao(fornecedor(), hash));

        assertThat(outra.getId()).isNotNull();
    }

    @Test
    void deveGerarChaveDoItemPelaReferenciaOuPelaDescricaoNormalizada() {
        Long fornecedorId = fornecedor();

        ApresentacaoFornecedor comReferencia = apresentacoes.saveAndFlush(
                apresentacao(fornecedorId, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.UNITARIO));
        ApresentacaoFornecedor semReferencia = apresentacoes.saveAndFlush(
                apresentacao(fornecedorId, null, "ACUCAR DE COCO", null, ModalidadePreco.ATACADO));

        assertThat(comReferencia.getChaveItem()).isEqualTo("1076");
        assertThat(semReferencia.getChaveItem()).isEqualTo("ACUCAR DE COCO");
    }

    @Test
    void deveBloquearApresentacaoRepetidaMesmoComUnidadeNula() {
        Long fornecedorId = fornecedor();
        apresentacoes.saveAndFlush(apresentacao(fornecedorId, null, "ACUCAR DE COCO", null, ModalidadePreco.VAREJO));

        assertThatThrownBy(() -> apresentacoes.saveAndFlush(
                        apresentacao(fornecedorId, null, "ACUCAR DE COCO", null, ModalidadePreco.VAREJO)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("uk_apresentacao_chave");
    }

    @Test
    void deveBloquearMesmaReferenciaComDescricaoDiferente() {
        Long fornecedorId = fornecedor();
        apresentacoes.saveAndFlush(apresentacao(fornecedorId, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.FARDO));

        assertThatThrownBy(() -> apresentacoes.saveAndFlush(
                        apresentacao(fornecedorId, "1076", "ACUCAR COCO 1KG", "KG", ModalidadePreco.FARDO)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("uk_apresentacao_chave");
    }

    @Test
    void devePermitirMesmaChaveComOutraUnidadeModalidadeOuFornecedor() {
        Long fornecedorId = fornecedor();
        apresentacoes.saveAndFlush(
                apresentacao(fornecedorId, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.UNITARIO));

        apresentacoes.saveAndFlush(
                apresentacao(fornecedorId, "1076", "ACUCAR DE COCO", "UN", ModalidadePreco.UNITARIO));
        apresentacoes.saveAndFlush(apresentacao(fornecedorId, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.FARDO));
        apresentacoes.saveAndFlush(
                apresentacao(fornecedor(), "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.UNITARIO));

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM apresentacao_fornecedor WHERE fornecedor_id = ?",
                        Integer.class,
                        fornecedorId))
                .isEqualTo(3);
    }

    @Test
    void deveGuardarTodosOsErrosDoItemEmJsonb() {
        Importacao importacao = importacoes.saveAndFlush(importacao(fornecedor(), hashUnico()));
        List<MensagemErro> erros = List.of(
                new MensagemErro("PRECO_AUSENTE", "Preço não informado"),
                new MensagemErro("DESCRICAO_VAZIA", "Descrição vazia"));

        itens.saveAndFlush(ItemImportado.registrar(
                importacao.getId(), itemExtraido(3, null, null), SituacaoItem.INVALIDO, erros, null));

        assertThat(itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(importacao.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getErros()).isEqualTo(erros);
                    assertThat(item.getSituacao()).isEqualTo(SituacaoItem.INVALIDO);
                    assertThat(item.getQuantidade()).isNull();
                });
        assertThat(jdbc.queryForObject(
                        "SELECT erros -> 1 ->> 'codigo' FROM item_importado WHERE importacao_id = ?",
                        String.class,
                        importacao.getId()))
                .isEqualTo("DESCRICAO_VAZIA");
    }

    @Test
    void deveBloquearItemRepetidoNaMesmaLinhaEModalidade() {
        UUID importacaoId =
                importacoes.saveAndFlush(importacao(fornecedor(), hashUnico())).getId();
        itens.saveAndFlush(ItemImportado.registrar(
                importacaoId, itemExtraido(5, ModalidadePreco.UNITARIO, "32"), SituacaoItem.VALIDO, List.of(), null));
        itens.saveAndFlush(ItemImportado.registrar(
                importacaoId, itemExtraido(5, ModalidadePreco.FARDO, "30"), SituacaoItem.VALIDO, List.of(), null));

        assertThatThrownBy(() -> itens.saveAndFlush(ItemImportado.registrar(
                        importacaoId,
                        itemExtraido(5, ModalidadePreco.UNITARIO, "31"),
                        SituacaoItem.VALIDO,
                        List.of(),
                        null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("uk_item_importado_linha_modalidade");
    }

    @Test
    void deveRecusarHistoricoComPrecoZero() {
        Long fornecedorId = fornecedor();
        Long apresentacaoId = apresentacoes
                .saveAndFlush(apresentacao(fornecedorId, "1077", "STEVIA", "UN", ModalidadePreco.UNITARIO))
                .getId();
        UUID importacaoId =
                importacoes.saveAndFlush(importacao(fornecedorId, hashUnico())).getId();

        assertThatThrownBy(() -> historicos.saveAndFlush(
                        HistoricoPreco.registrar(apresentacaoId, importacaoId, BigDecimal.ZERO, AGORA)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("ck_historico_preco_positivo");
    }

    @Test
    void deveBuscarUltimoPrecoValidoIgnorandoAImportacaoInformada() {
        Long fornecedorId = fornecedor();
        Long apresentacaoId = apresentacoes
                .saveAndFlush(apresentacao(fornecedorId, "1076", "ACUCAR DE COCO", "KG", ModalidadePreco.UNITARIO))
                .getId();
        Long semHistorico = apresentacoes
                .saveAndFlush(apresentacao(fornecedorId, "1077", "STEVIA", "UN", ModalidadePreco.UNITARIO))
                .getId();
        UUID primeira = registrarPreco(fornecedorId, apresentacaoId, "33.00", AGORA);
        UUID segunda = registrarPreco(fornecedorId, apresentacaoId, "35.00", AGORA.plusSeconds(60));
        UUID atual =
                importacoes.saveAndFlush(importacao(fornecedorId, hashUnico())).getId();

        assertThat(historicos.buscarUltimoPrecoValido(apresentacaoId, atual))
                .get()
                .satisfies(historico -> {
                    assertThat(historico.getPreco()).isEqualByComparingTo("35.00");
                    assertThat(historico.getImportacaoId()).isEqualTo(segunda);
                });
        assertThat(historicos.buscarUltimoPrecoValido(apresentacaoId, segunda))
                .get()
                .extracting(HistoricoPreco::getImportacaoId)
                .isEqualTo(primeira);
        assertThat(historicos.buscarUltimoPrecoValido(semHistorico, atual)).isEmpty();
    }

    private UUID registrarPreco(Long fornecedorId, Long apresentacaoId, String preco, Instant registradoEm) {
        UUID importacaoId =
                importacoes.saveAndFlush(importacao(fornecedorId, hashUnico())).getId();
        historicos.saveAndFlush(
                HistoricoPreco.registrar(apresentacaoId, importacaoId, new BigDecimal(preco), registradoEm));
        return importacaoId;
    }

    private void mudarStatus(Importacao importacao, String status, String motivo) {
        jdbc.update(
                "UPDATE importacao SET status = ?, motivo_falha = ? WHERE id = ?", status, motivo, importacao.getId());
    }

    private Long fornecedor() {
        return fornecedores
                .saveAndFlush(Fornecedor.cadastrar(
                        new DadosFornecedor("Fornecedor de teste", null, null, "TABELA_REFERENCIA", null), AGORA))
                .getId();
    }

    private static Importacao importacao(Long fornecedorId, String hash) {
        return Importacao.receber(fornecedorId, "Fornecedor_1.pdf", hash, 1024, AGORA);
    }

    private static ApresentacaoFornecedor apresentacao(
            Long fornecedorId, String referencia, String descricao, String unidade, ModalidadePreco modalidade) {
        return ApresentacaoFornecedor.nova(fornecedorId, referencia, descricao, descricao, unidade, modalidade, AGORA);
    }

    private static ItemExtraido itemExtraido(int linha, ModalidadePreco modalidade, String preco) {
        return new ItemExtraido(
                linha,
                "1076 ACUCAR DE COCO KG 32 30",
                "1076",
                "ACUCAR DE COCO",
                "KG",
                modalidade,
                null,
                preco == null ? null : new BigDecimal(preco),
                List.of());
    }

    private static String hashUnico() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");
    }
}
