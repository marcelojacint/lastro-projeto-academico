package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import br.edu.uniesp.lastro.compartilhado.erro.ProcessamentoFalhouException;
import br.edu.uniesp.lastro.importacao.aplicacao.ImportacaoComFornecedor;
import br.edu.uniesp.lastro.importacao.aplicacao.ImportacaoService;
import br.edu.uniesp.lastro.importacao.dominio.FiltroImportacao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.MotivoFalha;
import br.edu.uniesp.lastro.importacao.dominio.StatusImportacao;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {

    private final ImportacaoService service;

    public ImportacaoController(ImportacaoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoResposta> receber(
            @RequestParam(required = false) Long fornecedorId, @RequestParam(required = false) MultipartFile arquivo)
            throws IOException {
        ImportacaoComFornecedor resultado = service.receber(
                fornecedorId,
                arquivo == null ? null : arquivo.getOriginalFilename(),
                arquivo == null ? null : arquivo.getBytes());
        Importacao importacao = resultado.importacao();
        if (importacao.getStatus() == StatusImportacao.FALHOU) {
            MotivoFalha motivo = importacao.getMotivoFalha();
            throw new ProcessamentoFalhouException(
                    "A importação foi registrada, mas o PDF não pôde ser lido",
                    new ErroCampo("arquivo", motivo.getMensagem()),
                    Map.of("importacaoId", importacao.getId(), "motivoFalha", motivo));
        }
        return ResponseEntity.created(
                        URI.create("/api/importacoes/" + resultado.importacao().getId()))
                .body(ImportacaoResposta.de(resultado));
    }

    @GetMapping
    public List<ImportacaoResposta> listar(
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) StatusImportacao status) {
        return service.listar(new FiltroImportacao(fornecedorId, status)).stream()
                .map(ImportacaoResposta::de)
                .toList();
    }
}
