package br.edu.uniesp.lastro.fornecedor.api;

import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FornecedorResposta> cadastrar(@RequestBody FornecedorRequisicao requisicao) {
        Fornecedor fornecedor = service.cadastrar(requisicao.paraDados());
        return ResponseEntity.created(URI.create("/api/fornecedores/" + fornecedor.getId()))
                .body(FornecedorResposta.de(fornecedor));
    }

    @GetMapping
    public List<FornecedorResposta> listar() {
        return service.listar().stream().map(FornecedorResposta::de).toList();
    }

    @GetMapping("/{id}")
    public FornecedorResposta buscar(@PathVariable Long id) {
        return FornecedorResposta.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    public FornecedorResposta atualizar(@PathVariable Long id, @RequestBody FornecedorRequisicao requisicao) {
        return FornecedorResposta.de(service.atualizar(id, requisicao.paraDados()));
    }
}
