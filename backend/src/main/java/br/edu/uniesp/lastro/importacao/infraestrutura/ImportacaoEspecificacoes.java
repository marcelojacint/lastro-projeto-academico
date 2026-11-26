package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.FiltroImportacao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ImportacaoEspecificacoes {

    private ImportacaoEspecificacoes() {}

    public static Specification<Importacao> filtradoPor(FiltroImportacao filtro) {
        return (raiz, consulta, construtor) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.fornecedorId() != null) {
                condicoes.add(construtor.equal(raiz.get("fornecedorId"), filtro.fornecedorId()));
            }
            if (filtro.status() != null) {
                condicoes.add(construtor.equal(raiz.get("status"), filtro.status()));
            }
            return construtor.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
