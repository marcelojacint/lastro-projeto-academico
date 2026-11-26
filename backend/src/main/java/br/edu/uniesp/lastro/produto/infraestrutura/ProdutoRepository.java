package br.edu.uniesp.lastro.produto.infraestrutura;

import br.edu.uniesp.lastro.produto.dominio.Produto;
import br.edu.uniesp.lastro.produto.dominio.StatusProduto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    List<Produto> findByStatus(StatusProduto status);
}
