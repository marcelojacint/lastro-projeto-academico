package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VinculoProdutoTest {

    private static final Instant AGORA = Instant.parse("2026-11-27T10:00:00Z");
    private static final UUID IMPORTACAO = UUID.fromString("4f1c2a8e-0b7d-4c55-9a52-3f6b9d1e2a10");

    @Test
    void deveCriarVinculoManualAtivo() {
        VinculoProduto vinculo = VinculoProduto.manual(7L, 10L, AGORA);

        assertThat(vinculo.getOrigem()).isEqualTo(OrigemVinculo.MANUAL);
        assertThat(vinculo.isAtivo()).isTrue();
        assertThat(vinculo.getConfirmadoEm()).isEqualTo(AGORA);
        assertThat(vinculo.getDesativadoEm()).isNull();
    }

    @Test
    void deveDesativarGuardandoQuando() {
        VinculoProduto vinculo = VinculoProduto.automatico(7L, 10L, AGORA);

        vinculo.desativar(AGORA.plusSeconds(60));

        assertThat(vinculo.isAtivo()).isFalse();
        assertThat(vinculo.getDesativadoEm()).isEqualTo(AGORA.plusSeconds(60));
    }

    @Test
    void deveDeixarValidoSoOItemAssociadoSemErro() {
        ItemImportado semErro = item(List.of());
        ItemImportado comErro = item(List.of(ErroItem.PRECO_ZERO.paraMensagem()));

        semErro.marcarAssociado();
        comErro.marcarAssociado();

        assertThat(semErro.getSituacao()).isEqualTo(SituacaoItem.VALIDO);
        assertThat(comErro.getSituacao()).isEqualTo(SituacaoItem.INVALIDO);
    }

    @Test
    void deveExigirApresentacaoParaAssociar() {
        ItemImportado semApresentacao = ItemImportado.registrar(
                IMPORTACAO,
                new ItemExtraido(4, "Frete grátis", null, null, null, null, null, null, List.of()),
                SituacaoItem.INVALIDO,
                List.of(ErroItem.LINHA_FORA_DO_PADRAO.paraMensagem()),
                null);

        assertThatThrownBy(semApresentacao::exigirApresentacao).isInstanceOf(DadosInvalidosException.class);
    }

    private static ItemImportado item(List<MensagemErro> erros) {
        return ItemImportado.registrar(
                IMPORTACAO,
                new ItemExtraido(
                        2,
                        "1076 ACUCAR DE COCO KG 32 0",
                        "1076",
                        "ACUCAR DE COCO",
                        "KG",
                        ModalidadePreco.UNITARIO,
                        null,
                        new BigDecimal("32"),
                        List.of()),
                erros.isEmpty() ? SituacaoItem.PENDENTE_ASSOCIACAO : SituacaoItem.INVALIDO,
                erros,
                7L);
    }
}
