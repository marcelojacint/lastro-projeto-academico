package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.OrigemVinculo;
import java.time.Instant;

public record VinculoAtivo(
        Long id,
        Long apresentacaoId,
        String referencia,
        String descricao,
        String unidade,
        ModalidadePreco modalidadePreco,
        Long produtoId,
        OrigemVinculo origem,
        Instant confirmadoEm) {}
