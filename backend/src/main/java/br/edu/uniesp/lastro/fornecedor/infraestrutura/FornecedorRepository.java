package br.edu.uniesp.lastro.fornecedor.infraestrutura;

import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);
}
