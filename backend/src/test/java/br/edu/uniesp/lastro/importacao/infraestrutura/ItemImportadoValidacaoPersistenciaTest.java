package br.edu.uniesp.lastro.importacao.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.DadosFornecedor;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import br.edu.uniesp.lastro.fornecedor.infraestrutura.FornecedorRepository;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemExtraido;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ItemImportadoValidacaoPersistenciaTest {

    @Autowired
    private FornecedorRepository fornecedores;

    @Autowired
    private ImportacaoRepository importacoes;

    @Autowired
    private ItemImportadoRepository itens;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveGravarItemInvalidoComCodigoEMensagemDeCadaErro() {
        UUID importacaoId = novaImportacao();
        ItemExtraido semPrecoNemDescricao = new ItemExtraido(
                3, "1078 KG", "1078", null, "KG", ModalidadePreco.FARDO, null, BigDecimal.ZERO, List.of());

        itens.saveAndFlush(ItemImportado.registrarValidando(
                importacaoId, semPrecoNemDescricao, LayoutPdf.TABELA_REFERENCIA, null, false));

        ItemImportado gravado =
                itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(importacaoId).getFirst();
        assertThat(gravado.getSituacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThat(gravado.getErros())
                .containsExactly(
                        new MensagemErro("DESCRICAO_VAZIA", "Descrição vazia"),
                        new MensagemErro("PRECO_ZERO", "Preço igual a zero"));
        assertThat(jdbc.queryForObject(
                        "SELECT jsonb_array_length(erros) FROM item_importado WHERE importacao_id = ?",
                        Integer.class,
                        importacaoId))
                .isEqualTo(2);
    }

    @Test
    void deveGravarItemSemErrosComoPendenteDeAssociacao() {
        UUID importacaoId = novaImportacao();
        ItemExtraido correto = new ItemExtraido(
                2,
                "1076 ACUCAR DE COCO KG 32 32",
                "1076",
                "ACUCAR DE COCO",
                "KG",
                ModalidadePreco.UNITARIO,
                null,
                new BigDecimal("32"),
                List.of());

        itens.saveAndFlush(
                ItemImportado.registrarValidando(importacaoId, correto, LayoutPdf.TABELA_REFERENCIA, null, false));

        ItemImportado gravado =
                itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(importacaoId).getFirst();
        assertThat(gravado.getSituacao()).isEqualTo(SituacaoItem.PENDENTE_ASSOCIACAO);
        assertThat(gravado.getErros()).isEmpty();
    }

    private UUID novaImportacao() {
        Instant agora = Instant.now();
        Fornecedor fornecedor = fornecedores.saveAndFlush(Fornecedor.cadastrar(
                new DadosFornecedor("Validacao " + UUID.randomUUID(), null, null, "TABELA_REFERENCIA", true), agora));
        String hash = HexFormat.of()
                .formatHex(UUID.randomUUID().toString().getBytes())
                .substring(0, 64);
        return importacoes
                .saveAndFlush(Importacao.receber(fornecedor.getId(), "tabela.pdf", hash, 100, agora))
                .getId();
    }
}
