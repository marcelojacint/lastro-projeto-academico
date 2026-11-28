package br.edu.uniesp.lastro.precificacao.infraestrutura;

import br.edu.uniesp.lastro.precificacao.dominio.ComparacaoPreco;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComparacaoPrecoRepository extends JpaRepository<ComparacaoPreco, Long> {

    List<ComparacaoPreco> findByImportacaoId(UUID importacaoId);

    @Query("""
            select new br.edu.uniesp.lastro.precificacao.infraestrutura.ComparacaoComItem(c, i)
            from ComparacaoPreco c, ItemImportado i
            where i.id = c.itemImportadoId and c.importacaoId = :importacaoId
            order by i.numeroLinha, i.id
            """)
    List<ComparacaoComItem> listarComItens(@Param("importacaoId") UUID importacaoId);
}
