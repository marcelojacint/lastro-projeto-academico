package br.edu.uniesp.lastro.produto.aplicacao;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import br.edu.uniesp.lastro.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.uniesp.lastro.produto.dominio.DadosProduto;
import br.edu.uniesp.lastro.produto.dominio.Produto;
import br.edu.uniesp.lastro.produto.infraestrutura.ProdutoRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private static final String MENSAGEM_CODIGO_REPETIDO = "Já existe um produto com este código";

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Produto cadastrar(DadosProduto dados) {
        Produto produto = Produto.cadastrar(dados, Instant.now());
        String codigo = dados.codigoInformado();
        if (codigo != null && repository.existsByCodigo(codigo)) {
            throw new ConflitoException("codigo", MENSAGEM_CODIGO_REPETIDO);
        }
        return repository.save(produto);
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado"));
    }

    @Transactional
    public Produto atualizar(Long id, DadosProduto dados) {
        Produto produto = buscar(id);
        String codigo = dados.codigoInformado();
        if (codigo != null && repository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflitoException("codigo", MENSAGEM_CODIGO_REPETIDO);
        }
        produto.atualizar(dados, Instant.now());
        return repository.saveAndFlush(produto);
    }
}
