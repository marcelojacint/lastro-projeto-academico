package br.edu.uniesp.lastro.importacao.dominio;

import java.util.EnumSet;
import java.util.Set;

public enum StatusImportacao {
    RECEBIDA,
    EM_CONFERENCIA,
    CONCLUIDA,
    CONCLUIDA_COM_PENDENCIAS,
    FALHOU,
    DESCARTADA;

    public static Set<StatusImportacao> liberamReenvio() {
        return EnumSet.of(FALHOU, DESCARTADA);
    }
}
