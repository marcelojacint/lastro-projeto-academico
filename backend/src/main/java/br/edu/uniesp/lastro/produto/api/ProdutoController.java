package br.edu.uniesp.lastro.produto.api;

import br.edu.uniesp.lastro.compartilhado.paginacao.PaginaResposta;
import br.edu.uniesp.lastro.produto.aplicacao.ProdutoService;
import br.edu.uniesp.lastro.produto.dominio.FiltroProduto;
import br.edu.uniesp.lastro.produto.dominio.Produto;
import br.edu.uniesp.lastro.produto.dominio.StatusProduto;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProdutoResposta> cadastrar(@RequestBody ProdutoRequisicao requisicao) {
        Produto produto = service.cadastrar(requisicao.paraDados());
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.getId()))
                .body(ProdutoResposta.de(produto));
    }

    @GetMapping
    public PaginaResposta<ProdutoResposta> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) StatusProduto status,
            @RequestParam(defaultValue = "0") int page) {
        FiltroProduto filtro = new FiltroProduto(nome, categoria, status);
        return PaginaResposta.de(service.listar(filtro, page), ProdutoResposta::de);
    }

    @GetMapping("/{id}")
    public ProdutoResposta buscar(@PathVariable Long id) {
        return ProdutoResposta.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    public ProdutoResposta atualizar(@PathVariable Long id, @RequestBody ProdutoRequisicao requisicao) {
        return ProdutoResposta.de(service.atualizar(id, requisicao.paraDados()));
    }
}
