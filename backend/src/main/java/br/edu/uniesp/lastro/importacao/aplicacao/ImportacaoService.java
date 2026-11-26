package br.edu.uniesp.lastro.importacao.aplicacao;

import br.edu.uniesp.lastro.fornecedor.aplicacao.FornecedorService;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import br.edu.uniesp.lastro.importacao.dominio.FiltroImportacao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.SolicitacaoImportacao;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoEspecificacoes;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportacaoService {

    private static final Sort MAIS_RECENTE_PRIMEIRO = Sort.by(Sort.Order.desc("recebidaEm"), Sort.Order.desc("id"));

    private final ImportacaoRepository repository;
    private final FornecedorService fornecedorService;

    public ImportacaoService(ImportacaoRepository repository, FornecedorService fornecedorService) {
        this.repository = repository;
        this.fornecedorService = fornecedorService;
    }

    @Transactional
    public ImportacaoComFornecedor receber(Long fornecedorId, String nomeArquivo, byte[] conteudo) {
        SolicitacaoImportacao solicitacao = SolicitacaoImportacao.validar(fornecedorId, nomeArquivo, conteudo);
        Fornecedor fornecedor = fornecedorService.buscar(solicitacao.fornecedorId());
        solicitacao.exigirFornecedorAtivo(fornecedor);
        Importacao importacao = repository.save(Importacao.receber(
                fornecedor.getId(),
                solicitacao.nomeArquivo(),
                solicitacao.hashSha256(),
                solicitacao.tamanhoBytes(),
                Instant.now()));
        return new ImportacaoComFornecedor(importacao, fornecedor.getNome());
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
