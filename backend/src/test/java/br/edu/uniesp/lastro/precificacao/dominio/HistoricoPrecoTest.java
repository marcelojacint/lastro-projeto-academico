package br.edu.uniesp.lastro.precificacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import br.edu.uniesp.lastro.importacao.dominio.ItemExtraido;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HistoricoPrecoTest {

    private static final UUID IMPORTACAO = UUID.fromString("4f1c2a8e-0b7d-4c55-9a52-3f6b9d1e2a10");
    private static final Instant AGORA = Instant.parse("2026-11-26T10:00:00Z");

    @Test
    void deveRegistrarNoHistoricoItemValidoAssociadoComPreco() {
        ItemImportado valido = ItemImportado.registrar(IMPORTACAO, item("35.00"), SituacaoItem.VALIDO, List.of(), 12L);

        HistoricoPreco historico = HistoricoPreco.doItem(valido, AGORA);

        assertThat(historico.getApresentacaoId()).isEqualTo(12L);
        assertThat(historico.getImportacaoId()).isEqualTo(IMPORTACAO);
        assertThat(historico.getPreco()).isEqualByComparingTo("35.00");
        assertThat(historico.getRegistradoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveRecusarItemInvalidoNoHistorico() {
        ItemImportado invalido =
                ItemImportado.registrarValidando(IMPORTACAO, item("0"), LayoutPdf.TABELA_REFERENCIA, null, false);

        assertThat(invalido.getSituacao()).isEqualTo(SituacaoItem.INVALIDO);
        assertThatThrownBy(() -> HistoricoPreco.doItem(invalido, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALIDO");
    }

    @Test
    void deveRecusarItemPendenteDeAssociacaoNoHistorico() {
        ItemImportado pendente =
                ItemImportado.registrarValidando(IMPORTACAO, item("35.00"), LayoutPdf.TABELA_REFERENCIA, null, false);

        assertThat(pendente.getSituacao()).isEqualTo(SituacaoItem.PENDENTE_ASSOCIACAO);
        assertThatThrownBy(() -> HistoricoPreco.doItem(pendente, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDENTE_ASSOCIACAO");
    }

    @Test
    void deveRecusarItemMarcadoValidoSemPrecoPositivo() {
        ItemImportado inconsistente = ItemImportado.registrar(
                IMPORTACAO,
                item("0"),
                SituacaoItem.VALIDO,
                List.of(new MensagemErro("PRECO_ZERO", "Preço igual a zero")),
                12L);

        assertThatThrownBy(() -> HistoricoPreco.doItem(inconsistente, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ItemExtraido item(String preco) {
        return new ItemExtraido(
                2,
                "1076 ACUCAR DE COCO KG 35 32",
                "1076",
                "ACUCAR DE COCO",
                "KG",
                ModalidadePreco.UNITARIO,
                null,
                new BigDecimal(preco),
                List.of());
    }
}
