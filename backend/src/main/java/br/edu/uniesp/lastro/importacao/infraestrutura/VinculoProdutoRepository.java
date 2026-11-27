package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.VinculoProduto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoProdutoRepository extends JpaRepository<VinculoProduto, Long> {

    Optional<VinculoProduto> findByApresentacaoIdAndAtivoTrue(Long apresentacaoId);

    List<VinculoProduto> findByApresentacaoIdInAndAtivoTrue(Collection<Long> apresentacaoIds);
}
