package br.edu.uniesp.lastro.importacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AssociacaoManualImportacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveAssociarManualmenteERecalcularSituacaoEContadores() throws Exception {
        Integer produtoId = cadastrarProduto(nomeUnico("Stevia andina"), true);
        Integer fornecedorId = cadastrarFornecedor();
        String id = importar(fornecedorId, "1077 DOCANTE STEVIA COLOR ANDINA 40G UN 78 78");
        Integer unitario = itemDaPosicao(id, 0);

        associar(id, unitario, produtoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].situacao").value("VALIDO"))
                .andExpect(jsonPath("$.itens[0].produtoId").value(produtoId))
                .andExpect(jsonPath("$.itens[1].situacao").value("PENDENTE_ASSOCIACAO"))
                .andExpect(jsonPath("$.importacao.itensValidos").value(1))
                .andExpect(jsonPath("$.importacao.itensPendentes").value(1));

        assertThat(vinculosDaApresentacao(id, 0)).singleElement().satisfies(vinculo -> {
            assertThat(vinculo.get("origem")).isEqualTo("MANUAL");
            assertThat(vinculo.get("ativo")).isEqualTo(true);
        });
    }

    @Test
    void deveTrocarOProdutoDesativandoOVinculoAnterior() throws Exception {
        String nome = nomeUnico("Granola");
        cadastrarProduto(nome, true);
        Integer outro = cadastrarProduto(nomeUnico("Granola sem acucar"), true);
        Integer fornecedorId = cadastrarFornecedor();
        String id = importar(fornecedorId, "2001 " + nome.toUpperCase(Locale.ROOT) + " KG 30 28");
        Integer item = itemDaPosicao(id, 0);

        associar(id, item, outro).andExpect(jsonPath("$.itens[0].produtoId").value(outro));

        List<Map<String, Object>> historico = vinculosDaApresentacao(id, 0);
        assertThat(historico).hasSize(2);
        assertThat(historico.get(0)).containsEntry("origem", "AUTOMATICA").containsEntry("ativo", false);
        assertThat(historico.get(0).get("desativado_em")).isNotNull();
        assertThat(historico.get(1)).containsEntry("origem", "MANUAL").containsEntry("ativo", true);
    }

    @Test
    void deveUsarOVinculoManualNaProximaImportacaoDoFornecedor() throws Exception {
        Integer produtoId = cadastrarProduto(nomeUnico("Cacau em po"), true);
        Integer fornecedorId = cadastrarFornecedor();
        String primeira = importar(fornecedorId, "3001 CACAU 100 PORCENTO KG 50 48");
        associar(primeira, itemDaPosicao(primeira, 0), produtoId);
        associar(primeira, itemDaPosicao(primeira, 1), produtoId);

        String segunda = importar(fornecedorId, "3001 CACAU 100 PORCENTO KG 52 49");

        mockMvc.perform(get("/api/importacoes/{id}", segunda))
                .andExpect(jsonPath("$.itens[*].situacao", contains("VALIDO", "VALIDO")))
                .andExpect(jsonPath("$.itens[*].produtoId", contains(produtoId, produtoId)))
                .andExpect(jsonPath("$.importacao.itensValidos").value(2));
    }

    @Test
    void deveManterInvalidoOItemComErroMesmoAssociado() throws Exception {
        Integer produtoId = cadastrarProduto(nomeUnico("Mel"), true);
        Integer fornecedorId = cadastrarFornecedor();
        String id = importar(fornecedorId, "4001 MEL SILVESTRE 500G UN 25 0");

        associar(id, itemDaPosicao(id, 1), produtoId)
                .andExpect(jsonPath("$.itens[1].situacao").value("INVALIDO"))
                .andExpect(jsonPath("$.itens[1].produtoId").value(produtoId));
    }

    @Test
    void deveRecusarItemSemApresentacaoProdutoInativoEProdutoInexistente() throws Exception {
        Integer ativo = cadastrarProduto(nomeUnico("Linhaca"), true);
        Integer inativo = cadastrarProduto(nomeUnico("Linhaca velha"), false);
        Integer fornecedorId = cadastrarFornecedor();
        String id = importar(fornecedorId, "5001 LINHACA DOURADA KG 15 14", "Frete por conta do cliente");

        associar(id, itemDaPosicao(id, 2), ativo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("itemId"));
        associar(id, itemDaPosicao(id, 0), inativo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Produto inativo não pode ser associado"));
        associar(id, itemDaPosicao(id, 0), 999_999).andExpect(status().isNotFound());
    }

    @Test
    void deveResponderConflitoForaDeConferencia() throws Exception {
        Integer produtoId = cadastrarProduto(nomeUnico("Canela"), true);
        Integer fornecedorId = cadastrarFornecedor();
        String falhou = JsonPath.read(
                mockMvc.perform(multipart("/api/importacoes")
                                .file(new MockMultipartFile(
                                        "arquivo",
                                        "quebrado.pdf",
                                        "application/pdf",
                                        ("%PDF-1.4\n" + UUID.randomUUID()).getBytes(StandardCharsets.US_ASCII)))
                                .param("fornecedorId", fornecedorId.toString()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.importacaoId");

        associar(falhou, 1, produtoId).andExpect(status().isConflict());
    }

    @Test
    void deveListarSoOsVinculosAtivosDoFornecedor() throws Exception {
        String nome = nomeUnico("Castanha");
        Integer automatico = cadastrarProduto(nome, true);
        Integer manual = cadastrarProduto(nomeUnico("Castanha do para"), true);
        Integer fornecedorId = cadastrarFornecedor();
        Integer outroFornecedor = cadastrarFornecedor();
        String id = importar(fornecedorId, "6001 " + nome.toUpperCase(Locale.ROOT) + " KG 80 78");
        importar(outroFornecedor, "6001 " + nome.toUpperCase(Locale.ROOT) + " KG 80 78");
        associar(id, itemDaPosicao(id, 1), manual);

        mockMvc.perform(get("/api/vinculos").param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].modalidadePreco", contains("FARDO", "UNITARIO")))
                .andExpect(jsonPath("$[*].origem", contains("MANUAL", "AUTOMATICA")))
                .andExpect(jsonPath("$[*].produtoId", contains(manual, automatico)))
                .andExpect(jsonPath("$[0].produtoNome").value(startsWith("Castanha do para")))
                .andExpect(jsonPath("$[0].referencia").value("6001"));
        mockMvc.perform(get("/api/vinculos").param("fornecedorId", "999999")).andExpect(status().isNotFound());
    }

    private ResultActions associar(String importacaoId, Integer itemId, Integer produtoId) throws Exception {
        return mockMvc.perform(post("/api/importacoes/{id}/itens/{itemId}/vinculo", importacaoId, itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":%d}".formatted(produtoId)));
    }

    private String importar(Integer fornecedorId, String... linhas) throws Exception {
        String[] conteudo = new String[linhas.length + 1];
        conteudo[0] = PdfDeTeste.CABECALHO_REFERENCIA;
        System.arraycopy(linhas, 0, conteudo, 1, linhas.length);
        String resposta = mockMvc.perform(multipart("/api/importacoes")
                        .file(new MockMultipartFile(
                                "arquivo", "tabela.pdf", "application/pdf", PdfDeTeste.comLinhas(conteudo)))
                        .param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private Integer itemDaPosicao(String importacaoId, int posicao) throws Exception {
        String resposta = mockMvc.perform(get("/api/importacoes/{id}", importacaoId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.itens[" + posicao + "].id");
    }

    private List<Map<String, Object>> vinculosDaApresentacao(String importacaoId, int posicao) throws Exception {
        String resposta = mockMvc.perform(get("/api/importacoes/{id}", importacaoId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer apresentacaoId = JsonPath.read(resposta, "$.itens[" + posicao + "].apresentacaoId");
        return jdbc.queryForList(
                "SELECT origem, ativo, desativado_em FROM vinculo_produto WHERE apresentacao_id = ? ORDER BY id",
                apresentacaoId);
    }

    private static String nomeUnico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private Integer cadastrarProduto(String nome, boolean ativo) throws Exception {
        String resposta = mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"precoVenda\":10}".formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");
        if (!ativo) {
            mockMvc.perform(put("/api/produtos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"%s\",\"precoVenda\":10,\"status\":\"INATIVO\"}".formatted(nome)))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private Integer cadastrarFornecedor() throws Exception {
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Manual %s\",\"layoutPdf\":\"TABELA_REFERENCIA\",\"ativo\":true}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }
}
