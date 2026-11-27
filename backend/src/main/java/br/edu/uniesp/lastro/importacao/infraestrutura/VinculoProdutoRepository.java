package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.VinculoProduto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VinculoProdutoRepository extends JpaRepository<VinculoProduto, Long> {

    Optional<VinculoProduto> findByApresentacaoIdAndAtivoTrue(Long apresentacaoId);

    List<VinculoProduto> findByApresentacaoIdInAndAtivoTrue(Collection<Long> apresentacaoIds);

    @Query("""
            select new br.edu.uniesp.lastro.importacao.infraestrutura.VinculoAtivo(
                v.id, v.apresentacaoId, a.referencia, a.descricaoOriginal, a.unidade, a.modalidadePreco,
                v.produtoId, v.origem, v.confirmadoEm)
            from VinculoProduto v, ApresentacaoFornecedor a
            where a.id = v.apresentacaoId and v.ativo = true and a.fornecedorId = :fornecedorId
            order by a.descricaoNormalizada, a.modalidadePreco
            """)
    List<VinculoAtivo> listarAtivosDoFornecedor(@Param("fornecedorId") Long fornecedorId);
}
