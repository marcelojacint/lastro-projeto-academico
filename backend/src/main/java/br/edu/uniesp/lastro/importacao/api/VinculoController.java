package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.importacao.aplicacao.VinculoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vinculos")
public class VinculoController {

    private final VinculoService service;

    public VinculoController(VinculoService service) {
        this.service = service;
    }

    @GetMapping
    public List<VinculoResposta> listar(@RequestParam Long fornecedorId) {
        return service.listarAtivos(fornecedorId).stream()
                .map(VinculoResposta::de)
                .toList();
    }
}
