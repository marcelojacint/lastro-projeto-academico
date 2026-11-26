package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.fornecedor.dominio.LayoutPdf;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DetectorCabecalho {

    private static final Map<LayoutPdf, List<String>> PALAVRAS_DO_CABECALHO = Map.of(
            LayoutPdf.TABELA_REFERENCIA, List.of("REF.", "PRODUTO", "UNIDADE", "FARDO"),
            LayoutPdf.ATACADO_VAREJO, List.of("DESCRICAO", "ATACADO", "VAREJO"));

    private DetectorCabecalho() {}

    public static Optional<CabecalhoDetectado> detectar(List<String> linhas) {
        for (int indice = 0; indice < linhas.size(); indice++) {
            String normalizada = NormalizadorDescricao.normalizar(linhas.get(indice));
            if (normalizada == null) {
                continue;
            }
            Set<String> palavras = Set.copyOf(Arrays.asList(normalizada.split(" ")));
            for (Map.Entry<LayoutPdf, List<String>> layout : PALAVRAS_DO_CABECALHO.entrySet()) {
                if (palavras.containsAll(layout.getValue())) {
                    return Optional.of(new CabecalhoDetectado(layout.getKey(), indice));
                }
            }
        }
        return Optional.empty();
    }

    public record CabecalhoDetectado(LayoutPdf layout, int indice) {}
}
