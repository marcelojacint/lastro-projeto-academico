package br.edu.uniesp.lastro.fornecedor.aplicacao;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import br.edu.uniesp.lastro.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.uniesp.lastro.fornecedor.dominio.DadosFornecedor;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.fornecedor.infraestrutura.FornecedorRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FornecedorService {

    private static final String MENSAGEM_CNPJ_REPETIDO = "Já existe um fornecedor com este CNPJ";
    private static final Sort ORDENACAO_POR_NOME =
            Sort.by(Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("id"));

    private final FornecedorRepository repository;

    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Fornecedor cadastrar(DadosFornecedor dados) {
        Fornecedor fornecedor = Fornecedor.cadastrar(dados, Instant.now());
        if (fornecedor.getCnpj() != null && repository.existsByCnpj(fornecedor.getCnpj())) {
            throw new ConflitoException("cnpj", MENSAGEM_CNPJ_REPETIDO);
        }
        return repository.save(fornecedor);
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listar() {
        return repository.findAll(ORDENACAO_POR_NOME);
    }

    @Transactional(readOnly = true)
    public Fornecedor buscar(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor " + id + " não encontrado"));
    }

    @Transactional
    public Fornecedor atualizar(Long id, DadosFornecedor dados) {
        Fornecedor fornecedor = buscar(id);
        String cnpj = dados.cnpjInformado();
        if (cnpj != null && repository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new ConflitoException("cnpj", MENSAGEM_CNPJ_REPETIDO);
        }
        fornecedor.atualizar(dados, Instant.now());
        return repository.saveAndFlush(fornecedor);
    }
}
