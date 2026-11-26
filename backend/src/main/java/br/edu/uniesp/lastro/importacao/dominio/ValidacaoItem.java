package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.notificacao.MensagemErro;
import br.edu.uniesp.lastro.compartilhado.notificacao.Notificacao;
import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public final class ValidacaoItem {

    private static final String PREFIXO_ERRO_DE_PRECO = "PRECO_";

    private final Notificacao notificacao = new Notificacao();
    private final Set<String> codigos = new HashSet<>();

    private ValidacaoItem() {}

    public static ResultadoValidacaoItem validar(ItemExtraido item, LayoutPdf layout) {
        ValidacaoItem validacao = new ValidacaoItem();
        item.errosDeLeitura().forEach(validacao::adicionar);
        if (layout == LayoutPdf.TABELA_REFERENCIA) {
            validacao.exigirTexto(item.referencia(), ErroItem.REFERENCIA_AUSENTE);
        }
        validacao.exigirTexto(item.descricao(), ErroItem.DESCRICAO_VAZIA);
        if (layout == LayoutPdf.TABELA_REFERENCIA) {
            validacao.exigirTexto(item.unidade(), ErroItem.UNIDADE_AUSENTE);
        }
        if (item.modalidadePreco() == null) {
            validacao.adicionar(ErroItem.MODALIDADE_AUSENTE);
        }
        validacao.validarPreco(item.preco());
        return validacao.resultado();
    }

    private void validarPreco(BigDecimal preco) {
        if (preco == null) {
            if (codigos.stream().noneMatch(codigo -> codigo.startsWith(PREFIXO_ERRO_DE_PRECO))) {
                adicionar(ErroItem.PRECO_AUSENTE);
            }
            return;
        }
        int sinal = preco.signum();
        if (sinal == 0) {
            adicionar(ErroItem.PRECO_ZERO);
        } else if (sinal < 0) {
            adicionar(ErroItem.PRECO_NEGATIVO);
        }
    }

    private void exigirTexto(String valor, ErroItem erro) {
        if (valor == null || valor.isBlank()) {
            adicionar(erro);
        }
    }

    private void adicionar(ErroItem erro) {
        adicionar(new MensagemErro(erro.name(), erro.getMensagem()));
    }

    private void adicionar(MensagemErro erro) {
        if (codigos.add(erro.codigo())) {
            notificacao.adicionar(erro.codigo(), erro.mensagem());
        }
    }

    private ResultadoValidacaoItem resultado() {
        SituacaoItem situacao = notificacao.possuiErros() ? SituacaoItem.INVALIDO : SituacaoItem.PENDENTE_ASSOCIACAO;
        return new ResultadoValidacaoItem(situacao, notificacao.getErros());
    }
}
