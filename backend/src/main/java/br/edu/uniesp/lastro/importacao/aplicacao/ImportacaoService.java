package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.compartilhado.erro.ConflitoException;
import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.importacao.dominio.FiltroImportacao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.SolicitacaoImportacao;
import br.edu.uniesp.lastro.importacao.dominio.StatusImportacao;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoEspecificacoes;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ImportacaoService {

    private static final String INDICE_HASH_UNICO = "uk_importacao_fornecedor_hash";
    private static final String MENSAGEM_DUPLICADA = "Este arquivo já foi importado para este fornecedor";

    private static final Sort MAIS_RECENTE_PRIMEIRO = Sort.by(Sort.Order.desc("recebidaEm"), Sort.Order.desc("id"));

    private final ImportacaoRepository repository;
    private final FornecedorService fornecedorService;
    private final LeituraImportacao leitura;
    private final TransactionTemplate transacao;

    public ImportacaoService(
            ImportacaoRepository repository,
            FornecedorService fornecedorService,
            LeituraImportacao leitura,
            PlatformTransactionManager gerenciadorTransacao) {
        this.repository = repository;
        this.fornecedorService = fornecedorService;
        this.leitura = leitura;
        this.transacao = new TransactionTemplate(gerenciadorTransacao);
    }

    public ImportacaoComFornecedor receber(Long fornecedorId, String nomeArquivo, byte[] conteudo) {
        SolicitacaoImportacao solicitacao = SolicitacaoImportacao.validar(fornecedorId, nomeArquivo, conteudo);
        try {
            return transacao.execute(status -> registrar(solicitacao, conteudo));
        } catch (DataIntegrityViolationException violacao) {
            if (!violouHashUnico(violacao)) {
                throw violacao;
            }
            throw duplicada(importacaoAtiva(solicitacao).orElseThrow(() -> violacao));
        }
    }

    private ImportacaoComFornecedor registrar(SolicitacaoImportacao solicitacao, byte[] conteudo) {
        Fornecedor fornecedor = fornecedorService.buscar(solicitacao.fornecedorId());
        solicitacao.exigirFornecedorAtivo(fornecedor);
        importacaoAtiva(solicitacao).ifPresent(original -> {
            throw duplicada(original);
        });
        Importacao importacao = repository.saveAndFlush(Importacao.receber(
                fornecedor.getId(),
                solicitacao.nomeArquivo(),
                solicitacao.hashSha256(),
                solicitacao.tamanhoBytes(),
                Instant.now()));
        leitura.ler(importacao, fornecedor.getLayoutPdf(), conteudo);
        return new ImportacaoComFornecedor(importacao, fornecedor.getNome());
    }

    private Optional<Importacao> importacaoAtiva(SolicitacaoImportacao solicitacao) {
        return repository.findFirstByFornecedorIdAndHashSha256AndStatusNotIn(
                solicitacao.fornecedorId(), solicitacao.hashSha256(), StatusImportacao.liberamReenvio());
    }

    private static ConflitoException duplicada(Importacao original) {
        return new ConflitoException("arquivo", MENSAGEM_DUPLICADA, Map.of("importacaoId", original.getId()));
    }

    private static boolean violouHashUnico(DataIntegrityViolationException violacao) {
        return violacao.getCause() instanceof ConstraintViolationException restricao
                && INDICE_HASH_UNICO.equalsIgnoreCase(restricao.getConstraintName());
    }

    @Transactional(readOnly = true)
    public List<ImportacaoComFornecedor> listar(FiltroImportacao filtro) {
        Map<Long, String> nomesFornecedores =
                fornecedorService.listar().stream().collect(Collectors.toMap(Fornecedor::getId, Fornecedor::getNome));
        return repository.findAll(ImportacaoEspecificacoes.filtradoPor(filtro), MAIS_RECENTE_PRIMEIRO).stream()
                .map(importacao ->
                        new ImportacaoComFornecedor(importacao, nomesFornecedores.get(importacao.getFornecedorId())))
                .toList();
    }
}
