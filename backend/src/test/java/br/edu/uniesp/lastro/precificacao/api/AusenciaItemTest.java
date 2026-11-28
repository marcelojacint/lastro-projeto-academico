package br.edu.uniesp.lastro.precificacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.precificacao.dominio.AusenciaItem;
import br.edu.uniesp.lastro.precificacao.infraestrutura.AusenciaItemRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AusenciaItemTest {

    private static final String COMPARACAO = "/api/importacoes/{id}/comparacao";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AusenciaItemRepository ausencias;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveRegistrarAusenciaDaLinha1077SemAlterarNemDesativarOItem() throws Exception {
        Integer acucar = cadastrarProduto(nomeUnico("Acucar de coco"));
        Integer stevia = cadastrarProduto(nomeUnico("Stevia andina"));
        Integer fornecedorId = cadastrarFornecedor();
        String primeira = importarArquivo(fornecedorId, "Fornecedor_1.pdf");
        associarItensDaReferencia(primeira, "1076", acucar);
        associarItensDaReferencia(primeira, "1077", stevia);
        concluir(primeira);
        List<Long> apresentacoesDaStevia = apresentacoesDaReferencia(primeira, "1077");
        String segunda = importarArquivo(fornecedorId, "Fornecedor_1_v3.pdf");

        concluir(segunda);

        mockMvc.perform(get(COMPARACAO, segunda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(4)))
                .andExpect(jsonPath("$.itens[?(@.referencia == '1077')]", hasSize(0)))
                .andExpect(jsonPath("$.ausentes", hasSize(2)))
                .andExpect(jsonPath(
                        "$.ausentes[*].apresentacaoId",
                        contains(
                                apresentacoesDaStevia.get(0).intValue(),
                                apresentacoesDaStevia.get(1).intValue())))
                .andExpect(jsonPath("$.ausentes[*].referencia", everyItem(is("1077"))))
                .andExpect(jsonPath("$.ausentes[*].descricao", everyItem(is("DOCANTE STEVIA COLOR ANDINA 40G"))))
                .andExpect(jsonPath("$.ausentes[*].unidade", everyItem(is("UN"))))
                .andExpect(jsonPath("$.ausentes[*].modalidadePreco", contains("UNITARIO", "FARDO")))
                .andExpect(jsonPath("$.ausentes[*].produtoId", everyItem(is(stevia))))
                .andExpect(jsonPath("$.ausentes[*].ultimoPrecoValido", contains(78.0, 78.0)));

        assertThat(ausencias.findByImportacaoId(UUID.fromString(segunda)))
                .extracting(AusenciaItem::getApresentacaoId, AusenciaItem::getUltimoPrecoValido)
                .containsExactlyInAnyOrder(
                        tuple(apresentacoesDaStevia.get(0), new BigDecimal("78.00")),
                        tuple(apresentacoesDaStevia.get(1), new BigDecimal("78.00")));
        mockMvc.perform(get("/api/vinculos").param("fornecedorId", fornecedorId.toString()))
                .andExpect(jsonPath("$[?(@.referencia == '1077')].produtoId", contains(stevia, stevia)));
        assertThat(historicoDasApresentacoes(apresentacoesDaStevia))
                .containsExactly(new BigDecimal("78.00"), new BigDecimal("78.00"));
        assertThat(jdbc.queryForObject(
                        "select count(*) from apresentacao_fornecedor where fornecedor_id = ?",
                        Long.class,
                        fornecedorId))
                .isEqualTo(6);
    }

    @Test
    void deveUsarOUltimoPrecoValidoEIgnorarItemQueNuncaTeveHistorico() throws Exception {
        String cacau = nomeUnico("Cacau");
        cadastrarProduto(cacau);
        String semProduto = "SEM PRODUTO " + sufixo().toUpperCase(Locale.ROOT);
        Integer fornecedorId = cadastrarFornecedor();
        String primeira =
                importarLinhas(fornecedorId, linha("8001", cacau, "40 38"), linha("8002", semProduto, "10 9"));
        concluir(primeira);
        concluir(importarLinhas(fornecedorId, linha("8001", cacau, "42 39"), linha("8002", semProduto, "11 9")));
        String terceira = importarLinhas(fornecedorId, linha("8003", nomeUnico("Outro item"), "5 5"));

        concluir(terceira);

        mockMvc.perform(get(COMPARACAO, primeira)).andExpect(jsonPath("$.ausentes", hasSize(0)));
        mockMvc.perform(get(COMPARACAO, terceira).param("classificacao", "AUMENTOU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.ausentes", hasSize(2)))
                .andExpect(jsonPath("$.ausentes[*].referencia", everyItem(is("8001"))))
                .andExpect(jsonPath("$.ausentes[*].modalidadePreco", contains("UNITARIO", "FARDO")))
                .andExpect(jsonPath("$.ausentes[*].ultimoPrecoValido", contains(42.0, 39.0)));
    }

    private List<BigDecimal> historicoDasApresentacoes(List<Long> apresentacoes) {
        return jdbc.queryForList(
                "select preco from historico_preco where apresentacao_id in (?, ?) order by apresentacao_id",
                BigDecimal.class,
                apresentacoes.get(0),
                apresentacoes.get(1));
    }

    private void associarItensDaReferencia(String importacaoId, String referencia, Integer produtoId) throws Exception {
        List<Integer> itens =
                JsonPath.read(detalhe(importacaoId), "$.itens[?(@.referencia == '" + referencia + "')].id");
        for (Integer itemId : itens) {
            mockMvc.perform(post("/api/importacoes/{id}/itens/{itemId}/vinculo", importacaoId, itemId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"produtoId\":%d}".formatted(produtoId)))
                    .andExpect(status().isOk());
        }
    }

    private List<Long> apresentacoesDaReferencia(String importacaoId, String referencia) throws Exception {
        List<Integer> apresentacoes =
                JsonPath.read(detalhe(importacaoId), "$.itens[?(@.referencia == '" + referencia + "')].apresentacaoId");
        return apresentacoes.stream().map(Integer::longValue).sorted().toList();
    }

    private String detalhe(String importacaoId) throws Exception {
        return mockMvc.perform(get("/api/importacoes/{id}", importacaoId))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void concluir(String importacaoId) throws Exception {
        mockMvc.perform(post("/api/importacoes/{id}/concluir", importacaoId)).andExpect(status().isOk());
    }

    private String importarArquivo(Integer fornecedorId, String nomeArquivo) throws Exception {
        return importar(fornecedorId, nomeArquivo, PdfDeTeste.recurso(nomeArquivo));
    }

    private String importarLinhas(Integer fornecedorId, String... linhas) throws Exception {
        String[] conteudo = Stream.concat(Stream.of(PdfDeTeste.CABECALHO_REFERENCIA), Stream.of(linhas))
                .toArray(String[]::new);
        return importar(fornecedorId, "tabela.pdf", PdfDeTeste.comLinhas(conteudo));
    }

    private String importar(Integer fornecedorId, String nomeArquivo, byte[] pdf) throws Exception {
        String resposta = mockMvc.perform(multipart("/api/importacoes")
                        .file(new MockMultipartFile("arquivo", nomeArquivo, "application/pdf", pdf))
                        .param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private Integer cadastrarProduto(String nome) throws Exception {
        String resposta = mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"precoVenda\":10}".formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private Integer cadastrarFornecedor() throws Exception {
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ausencia %s\",\"layoutPdf\":\"TABELA_REFERENCIA\",\"ativo\":true}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private static String linha(String referencia, String descricao, String precos) {
        return referencia + " " + descricao.toUpperCase(Locale.ROOT) + " KG " + precos;
    }

    private static String nomeUnico(String base) {
        return base + " " + sufixo();
    }

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
