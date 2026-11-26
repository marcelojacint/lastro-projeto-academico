package br.edu.uniesp.lastro.compartilhado.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NotificacaoTest {

    @Test
    void deveComecarSemErros() {
        Notificacao notificacao = new Notificacao();

        assertThat(notificacao.possuiErros()).isFalse();
        assertThat(notificacao.getErros()).isEmpty();
    }

    @Test
    void deveAcumularTodosOsErrosNaOrdemEmQueForamAdicionados() {
        Notificacao notificacao = new Notificacao();

        notificacao.adicionar("PRECO_AUSENTE", "Preço não informado");
        notificacao.adicionar("DESCRICAO_VAZIA", "Descrição vazia");

        assertThat(notificacao.possuiErros()).isTrue();
        assertThat(notificacao.getErros())
                .containsExactly(
                        new MensagemErro("PRECO_AUSENTE", "Preço não informado"),
                        new MensagemErro("DESCRICAO_VAZIA", "Descrição vazia"));
    }

    @Test
    void naoDevePermitirAlterarOsErrosPorFora() {
        Notificacao notificacao = new Notificacao();
        notificacao.adicionar("PRECO_AUSENTE", "Preço não informado");

        assertThatThrownBy(() -> notificacao.getErros().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
