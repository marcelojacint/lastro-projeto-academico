package br.edu.uniesp.lastro.produto.infraestrutura;

import br.edu.uniesp.lastro.produto.dominio.FiltroProduto;
import br.edu.uniesp.lastro.produto.dominio.Produto;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProdutoEspecificacoes {

    private static final char CARACTERE_ESCAPE = '\\';

    private ProdutoEspecificacoes() {}

    public static Specification<Produto> filtradoPor(FiltroProduto filtro) {
        return (raiz, consulta, construtor) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.nome() != null) {
                condicoes.add(
                        construtor.like(construtor.lower(raiz.get("nome")), contendo(filtro.nome()), CARACTERE_ESCAPE));
            }
            if (filtro.categoria() != null) {
                condicoes.add(construtor.equal(
                        construtor.lower(raiz.get("categoria")),
                        filtro.categoria().toLowerCase(Locale.ROOT)));
            }
            if (filtro.status() != null) {
                condicoes.add(construtor.equal(raiz.get("status"), filtro.status()));
            }
            return construtor.and(condicoes.toArray(Predicate[]::new));
        };
    }

    private static String contendo(String texto) {
        String semCuringas = texto.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + semCuringas + "%";
    }
}
