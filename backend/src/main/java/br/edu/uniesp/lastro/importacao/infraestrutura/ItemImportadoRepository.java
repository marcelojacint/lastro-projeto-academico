package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImportadoRepository extends JpaRepository<ItemImportado, Long> {

    List<ItemImportado> findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID importacaoId);
}
