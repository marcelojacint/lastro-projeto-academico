package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.ApresentacaoFornecedor;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApresentacaoFornecedorRepository extends JpaRepository<ApresentacaoFornecedor, Long> {

    Optional<ApresentacaoFornecedor> findByFornecedorIdAndChaveItemAndUnidadeAndModalidadePreco(
            Long fornecedorId, String chaveItem, String unidade, ModalidadePreco modalidadePreco);
}
