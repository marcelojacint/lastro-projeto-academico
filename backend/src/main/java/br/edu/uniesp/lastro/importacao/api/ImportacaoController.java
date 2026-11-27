package br.edu.uniesp.lastro.importacao.api;

import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import br.edu.uniesp.lastro.compartilhado.erro.ProcessamentoFalhouException;
import br.edu.uniesp.lastro.importacao.aplicacao.ConclusaoService;
import br.edu.uniesp.lastro.importacao.aplicacao.ConferenciaService;
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
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {

    private final ImportacaoService service;
    private final ConferenciaService conferencia;
    private final ConclusaoService conclusao;

    public ImportacaoController(ImportacaoService service, ConferenciaService conferencia, ConclusaoService conclusao) {
        this.service = service;
        this.conferencia = conferencia;
        this.conclusao = conclusao;
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

    @GetMapping("/{id}")
    public ImportacaoDetalheResposta detalhar(@PathVariable UUID id) {
        return ImportacaoDetalheResposta.de(conferencia.detalhar(id));
    }

    @PutMapping("/{id}/itens/{itemId}")
    public ImportacaoDetalheResposta corrigirItem(
            @PathVariable UUID id, @PathVariable Long itemId, @RequestBody CorrecaoItemRequisicao requisicao) {
        return ImportacaoDetalheResposta.de(conferencia.corrigir(id, itemId, requisicao.paraCorrecao()));
    }

    @PostMapping("/{id}/itens/{itemId}/vinculo")
    public ImportacaoDetalheResposta associarItem(
            @PathVariable UUID id, @PathVariable Long itemId, @RequestBody AssociacaoRequisicao requisicao) {
        return ImportacaoDetalheResposta.de(conferencia.associar(id, itemId, requisicao.produtoId()));
    }

    @PostMapping("/{id}/concluir")
    public ImportacaoResposta concluir(@PathVariable UUID id) {
        return ImportacaoResposta.de(conclusao.concluir(id));
    }

    @PostMapping("/{id}/descartar")
    public ImportacaoResposta descartar(@PathVariable UUID id) {
        return ImportacaoResposta.de(conclusao.descartar(id));
    }
}
