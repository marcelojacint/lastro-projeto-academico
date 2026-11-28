package br.edu.uniesp.lastro.precificacao.infraestrutura;

import br.edu.uniesp.lastro.precificacao.dominio.HistoricoPreco;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoricoPrecoRepository extends JpaRepository<HistoricoPreco, Long> {

    default Optional<HistoricoPreco> buscarUltimoPrecoValido(Long apresentacaoId, UUID importacaoIgnorada) {
        return findFirstByApresentacaoIdAndImportacaoIdNotOrderByRegistradoEmDescIdDesc(
                apresentacaoId, importacaoIgnorada);
    }

    Optional<HistoricoPreco> findFirstByApresentacaoIdAndImportacaoIdNotOrderByRegistradoEmDescIdDesc(
            Long apresentacaoId, UUID importacaoIgnorada);

    @Query(value = """
            select distinct on (h.apresentacao_id) h.*
            from historico_preco h
            join apresentacao_fornecedor a on a.id = h.apresentacao_id
            where a.fornecedor_id = :fornecedorId and h.importacao_id <> :importacaoIgnorada
            order by h.apresentacao_id, h.registrado_em desc, h.id desc
            """, nativeQuery = true)
    List<HistoricoPreco> listarUltimosPrecosValidosDoFornecedor(
            @Param("fornecedorId") Long fornecedorId, @Param("importacaoIgnorada") UUID importacaoIgnorada);
}
