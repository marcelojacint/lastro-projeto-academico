package br.edu.uniesp.lastro.compartilhado.paginacao;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PaginaResposta<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {

    public static <E, T> PaginaResposta<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResposta<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
