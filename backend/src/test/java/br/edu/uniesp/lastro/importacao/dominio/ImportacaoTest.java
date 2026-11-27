package br.edu.uniesp.lastro.importacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportacaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-27T10:00:00.123456789Z");

    @Test
    void deveConcluirSemPendenciasQuandoTodosOsItensSaoValidos() {
        Importacao importacao = emConferencia();

        importacao.concluir(false, AGORA);

        assertThat(importacao.getStatus()).isEqualTo(StatusImportacao.CONCLUIDA);
        assertThat(importacao.getConcluidaEm()).isEqualTo(Instant.parse("2026-11-27T10:00:00.123456Z"));
    }

    @Test
    void deveConcluirComPendenciasQuandoSobraItemPendenteOuInvalido() {
        Importacao importacao = emConferencia();

        importacao.concluir(true, AGORA);

        assertThat(importacao.getStatus()).isEqualTo(StatusImportacao.CONCLUIDA_COM_PENDENCIAS);
    }

    @Test
    void deveDescartarSemPreencherConclusao() {
        Importacao importacao = emConferencia();

        importacao.descartar();

        assertThat(importacao.getStatus()).isEqualTo(StatusImportacao.DESCARTADA);
        assertThat(importacao.getConcluidaEm()).isNull();
    }

    @Test
    void deveRecusarConcluirOuDescartarForaDeConferencia() {
        Importacao recebida = Importacao.receber(1L, "tabela.pdf", "a".repeat(64), 10, AGORA);
        Importacao concluida = emConferencia();
        concluida.concluir(false, AGORA);
        Importacao descartada = emConferencia();
        descartada.descartar();
        Importacao falhou = emConferencia();
        falhou.falhar(MotivoFalha.PDF_ILEGIVEL);

        for (Importacao importacao : List.of(recebida, concluida, descartada, falhou)) {
            StatusImportacao antes = importacao.getStatus();
            assertThatThrownBy(() -> importacao.concluir(false, AGORA))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessage("Só dá para concluir uma importação em conferência");
            assertThatThrownBy(importacao::descartar)
                    .isInstanceOf(ConflitoException.class)
                    .hasMessage("Só dá para descartar uma importação em conferência");
            assertThat(importacao.getStatus()).isEqualTo(antes);
        }
    }

    private static Importacao emConferencia() {
        Importacao importacao = Importacao.receber(1L, "tabela.pdf", "a".repeat(64), 10, AGORA);
        importacao.iniciarConferencia(2, 1);
        return importacao;
    }
}
