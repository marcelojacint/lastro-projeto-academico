package br.edu.uniesp.lastro.precificacao.infraestrutura;

import br.edu.uniesp.lastro.precificacao.dominio.AusenciaItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AusenciaItemRepository extends JpaRepository<AusenciaItem, Long> {

    List<AusenciaItem> findByImportacaoId(UUID importacaoId);

    @Query("""
            select new br.edu.uniesp.lastro.precificacao.infraestrutura.AusenciaComApresentacao(a, ap)
            from AusenciaItem a, ApresentacaoFornecedor ap
            where ap.id = a.apresentacaoId and a.importacaoId = :importacaoId
            order by ap.id
            """)
    List<AusenciaComApresentacao> listarComApresentacoes(@Param("importacaoId") UUID importacaoId);
}
