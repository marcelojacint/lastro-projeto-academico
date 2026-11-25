package br.edu.uniesp.lastro.importacao.infraestrutura;

import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoRepository extends JpaRepository<Importacao, UUID> {}
