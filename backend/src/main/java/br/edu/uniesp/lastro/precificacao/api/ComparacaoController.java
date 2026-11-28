package br.edu.uniesp.lastro.precificacao.api;

import br.edu.uniesp.lastro.precificacao.aplicacao.ConsultaComparacaoService;
import br.edu.uniesp.lastro.precificacao.dominio.Classificacao;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComparacaoController {

    private final ConsultaComparacaoService service;

    public ComparacaoController(ConsultaComparacaoService service) {
        this.service = service;
    }

    @GetMapping("/api/importacoes/{id}/comparacao")
    public ComparacaoResposta consultar(
            @PathVariable UUID id, @RequestParam(required = false) Classificacao classificacao) {
        return ComparacaoResposta.de(service.consultar(id, classificacao));
    }
}
