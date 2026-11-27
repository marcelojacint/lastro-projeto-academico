package br.edu.uniesp.lastro.importacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.precificacao.dominio.HistoricoPreco;
import br.edu.uniesp.lastro.precificacao.infraestrutura.HistoricoPrecoRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConclusaoImportacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoricoPrecoRepository historicos;

    @Test
    void deveGravarSoItensValidosEConcluirComPendencias() throws Exception {
        String nome = nomeUnico("Castanha do para");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Conclusao pendencias");
        String id = importar(
                fornecedorId,
                PdfDeTeste.CABECALHO_REFERENCIA,
                "2001 " + nome.toUpperCase(Locale.ROOT) + " KG 80,50 0",
                "2002 " + nomeUnico("SEM PRODUTO") + " KG 10 9",
                "Frete por conta do cliente");

        concluir(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CONCLUIDA_COM_PENDENCIAS"))
                .andExpect(jsonPath("$.concluidaEm").value(notNullValue()));

        List<HistoricoPreco> gravados = historicoDa(id);
        assertThat(gravados)
                .singleElement()
                .satisfies(historico -> assertThat(historico.getPreco()).isEqualByComparingTo("80.50"));
        mockMvc.perform(get("/api/importacoes/{id}", id))
                .andExpect(jsonPath("$.importacao.status").value("CONCLUIDA_COM_PENDENCIAS"))
                .andExpect(jsonPath("$.itens[0].situacao").value("VALIDO"))
                .andExpect(jsonPath("$.itens[0].apresentacaoId")
                        .value(gravados.getFirst().getApresentacaoId().intValue()));
    }

    @Test
    void deveConcluirSemPendenciasQuandoTodosOsItensSaoValidos() throws Exception {
        String nome = nomeUnico("Amendoim torrado");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Conclusao completa");
        String id = importar(
                fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "2101 " + nome.toUpperCase(Locale.ROOT) + " KG 30 28");

        concluir(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));

        assertThat(historicoDa(id))
                .extracting(HistoricoPreco::getPreco)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("30"), new BigDecimal("28"));
    }

    @Test
    void deveManterIntactoOHistoricoDeImportacoesAnteriores() throws Exception {
        String nome = nomeUnico("Uva passa");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Conclusao historico");
        String linha = "2201 " + nome.toUpperCase(Locale.ROOT) + " KG %s %s";
        String primeira = importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, linha.formatted("20", "19"));
        concluir(primeira).andExpect(status().isOk());
        List<HistoricoPreco> antes = historicoDa(primeira);

        String segunda = importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, linha.formatted("25", "24"));
        concluir(segunda).andExpect(status().isOk());

        assertThat(historicoDa(primeira))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(antes);
        assertThat(historicoDa(segunda))
                .extracting(HistoricoPreco::getApresentacaoId)
                .containsExactlyInAnyOrderElementsOf(
                        antes.stream().map(HistoricoPreco::getApresentacaoId).toList());
    }

    @Test
    void deveDescartarSemGravarHistorico() throws Exception {
        String nome = nomeUnico("Coco ralado");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Descarte");
        String id = importar(
                fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "2301 " + nome.toUpperCase(Locale.ROOT) + " KG 15 14");

        descartar(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESCARTADA"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()));

        assertThat(historicoDa(id)).isEmpty();
    }

    @Test
    void deveResponderConflitoAoConcluirOuDescartarImportacaoJaConcluida() throws Exception {
        String nome = nomeUnico("Damasco seco");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Conclusao repetida");
        String id = importar(
                fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "2401 " + nome.toUpperCase(Locale.ROOT) + " KG 60 58");
        concluir(id).andExpect(status().isOk());

        concluir(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("status"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Só dá para concluir uma importação em conferência"));
        descartar(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Só dá para descartar uma importação em conferência"));

        assertThat(historicoDa(id)).hasSize(2);
    }

    @Test
    void deveResponderConflitoAoConcluirImportacaoDescartadaOuQueFalhou() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Conclusao fora de conferencia");
        String descartada =
                importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "2501 " + nomeUnico("FIGO") + " KG 70 68");
        descartar(descartada).andExpect(status().isOk());
        String falhou = importarComFalha(fornecedorId);

        concluir(descartada).andExpect(status().isConflict());
        descartar(descartada).andExpect(status().isConflict());
        concluir(falhou).andExpect(status().isConflict());
        descartar(falhou).andExpect(status().isConflict());

        assertThat(historicoDa(descartada)).isEmpty();
        assertThat(historicoDa(falhou)).isEmpty();
    }

    @Test
    void deveResponderConflitoQuandoDuasLinhasValidasTemOMesmoItemDoFornecedor() throws Exception {
        String nome = nomeUnico("Tamara");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Conclusao linha repetida");
        String linha = "2601 " + nome.toUpperCase(Locale.ROOT) + " KG %s %s";
        String id = importar(
                fornecedorId,
                PdfDeTeste.CABECALHO_REFERENCIA,
                linha.formatted("40", "38"),
                linha.formatted("41", "39"));

        concluir(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("itens"));

        assertThat(historicoDa(id)).isEmpty();
        mockMvc.perform(get("/api/importacoes/{id}", id))
                .andExpect(jsonPath("$.importacao.status").value("EM_CONFERENCIA"));
    }

    @Test
    void deveResponderNaoEncontradoParaImportacaoInexistente() throws Exception {
        concluir(UUID.randomUUID().toString()).andExpect(status().isNotFound());
        descartar(UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }

    private List<HistoricoPreco> historicoDa(String importacaoId) {
        UUID id = UUID.fromString(importacaoId);
        return historicos.findAll().stream()
                .filter(historico -> historico.getImportacaoId().equals(id))
                .toList();
    }

    private ResultActions concluir(String importacaoId) throws Exception {
        return mockMvc.perform(post("/api/importacoes/{id}/concluir", importacaoId));
    }

    private ResultActions descartar(String importacaoId) throws Exception {
        return mockMvc.perform(post("/api/importacoes/{id}/descartar", importacaoId));
    }

    private String importar(Integer fornecedorId, String... linhas) throws Exception {
        String resposta = mockMvc.perform(multipart("/api/importacoes")
                        .file(new MockMultipartFile(
                                "arquivo", "tabela.pdf", "application/pdf", PdfDeTeste.comLinhas(linhas)))
                        .param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private String importarComFalha(Integer fornecedorId) throws Exception {
        String resposta = mockMvc.perform(multipart("/api/importacoes")
                        .file(new MockMultipartFile(
                                "arquivo",
                                "quebrado.pdf",
                                "application/pdf",
                                ("%PDF-1.4\n" + UUID.randomUUID()).getBytes(StandardCharsets.US_ASCII)))
                        .param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isUnprocessableContent())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.importacaoId");
    }

    private static String nomeUnico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private void cadastrarProduto(String nome) throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"precoVenda\":10}".formatted(nome)))
                .andExpect(status().isCreated());
    }

    private Integer cadastrarFornecedor(String prefixo) throws Exception {
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s %s\",\"layoutPdf\":\"TABELA_REFERENCIA\",\"ativo\":true}"
                                .formatted(prefixo, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }
}
