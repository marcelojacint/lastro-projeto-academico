package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.StatusImportacao;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface ImportacaoRepository extends JpaRepository<Importacao, UUID>, JpaSpecificationExecutor<Importacao> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Importacao> findComBloqueioById(UUID id);

    Optional<Importacao> findFirstByFornecedorIdAndHashSha256AndStatusNotIn(
            Long fornecedorId, String hashSha256, Collection<StatusImportacao> status);
}
