package br.edu.uniesp.lastro.precificacao.infraestrutura;

import br.edu.uniesp.lastro.precificacao.dominio.HistoricoPreco;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoPrecoRepository extends JpaRepository<HistoricoPreco, Long> {

    default Optional<HistoricoPreco> buscarUltimoPrecoValido(Long apresentacaoId, UUID importacaoIgnorada) {
        return findFirstByApresentacaoIdAndImportacaoIdNotOrderByRegistradoEmDescIdDesc(
                apresentacaoId, importacaoIgnorada);
    }

    Optional<HistoricoPreco> findFirstByApresentacaoIdAndImportacaoIdNotOrderByRegistradoEmDescIdDesc(
            Long apresentacaoId, UUID importacaoIgnorada);
}
