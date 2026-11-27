package br.edu.uniesp.lastro.importacao.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class ConferenciaImportacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveDetalharCabecalhoContadoresEItensComSituacaoErrosEProduto() throws Exception {
        String nome = nomeUnico("Cha de hibisco");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Conferencia");
        String id = importar(
                fornecedorId,
                PdfDeTeste.CABECALHO_REFERENCIA,
                "1078 " + nome.toUpperCase(Locale.ROOT) + " UN 20 0",
                "Frete por conta do cliente");

        mockMvc.perform(get("/api/importacoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importacao.id").value(id))
                .andExpect(jsonPath("$.importacao.fornecedorNome").value(startsWith("Conferencia")))
                .andExpect(jsonPath("$.importacao.status").value("EM_CONFERENCIA"))
                .andExpect(jsonPath("$.importacao.totalItens").value(3))
                .andExpect(jsonPath("$.importacao.itensValidos").value(1))
                .andExpect(jsonPath("$.importacao.itensPendentes").value(2))
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.itens[*].numeroLinha", contains(2, 2, 3)))
                .andExpect(jsonPath("$.itens[*].modalidadePreco", contains("UNITARIO", "FARDO", null)))
                .andExpect(jsonPath("$.itens[*].situacao", contains("VALIDO", "INVALIDO", "INVALIDO")))
                .andExpect(jsonPath("$.itens[0].produtoNome").value(nome))
                .andExpect(jsonPath("$.itens[0].quantidade").value(nullValue()))
                .andExpect(jsonPath("$.itens[1].erros[0].codigo").value("PRECO_ZERO"))
                .andExpect(jsonPath("$.itens[1].erros[0].mensagem").value("Preço igual a zero"))
                .andExpect(jsonPath("$.itens[2].linhaOriginal").value("Frete por conta do cliente"))
                .andExpect(jsonPath("$.itens[2].produtoNome").value(nullValue()));
    }

    @Test
    void deveResponderNaoEncontradoParaImportacaoInexistente() throws Exception {
        mockMvc.perform(get("/api/importacoes/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void deveCorrigirPrecoRevalidarERecalcularContadores() throws Exception {
        String nome = nomeUnico("Mel silvestre");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Correcao preco");
        String id = importar(
                fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "5001 " + nome.toUpperCase(Locale.ROOT) + " UN 25 0");
        Integer itemFardo = itemDaPosicao(id, 1);

        corrigir(id, itemFardo, "5001", nome.toUpperCase(Locale.ROOT), "UN", "22.50")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[1].situacao").value("VALIDO"))
                .andExpect(jsonPath("$.itens[1].preco").value(22.5))
                .andExpect(jsonPath("$.itens[1].erros", hasSize(0)))
                .andExpect(jsonPath("$.importacao.itensValidos").value(2))
                .andExpect(jsonPath("$.importacao.itensPendentes").value(0));
    }

    @Test
    void deveAssociarDepoisQueADescricaoCorrigidaBateComUmProduto() throws Exception {
        String nome = nomeUnico("Granola sem acucar");
        Integer produtoId = cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor("Correcao descricao");
        String id = importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "7001 GRANOLA S ACUCAR KG 40 38");
        Integer item = itemDaPosicao(id, 0);

        corrigir(id, item, "7001", nome, "KG", "40")
                .andExpect(jsonPath("$.itens[0].situacao").value("VALIDO"))
                .andExpect(jsonPath("$.itens[0].produtoId").value(produtoId))
                .andExpect(jsonPath("$.itens[0].produtoNome").value(nome))
                .andExpect(jsonPath("$.itens[1].situacao").value("PENDENTE_ASSOCIACAO"))
                .andExpect(jsonPath("$.importacao.itensValidos").value(1));
    }

    @Test
    void deveDeixarInvalidoQuandoACorrecaoApagaADescricao() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Correcao invalida");
        String id = importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "8001 " + nomeUnico("AVEIA") + " KG 9 8");
        Integer item = itemDaPosicao(id, 0);

        corrigir(id, item, "8001", " ", "KG", "9")
                .andExpect(jsonPath("$.itens[0].situacao").value("INVALIDO"))
                .andExpect(jsonPath("$.itens[0].erros[*].codigo", contains("DESCRICAO_VAZIA")))
                .andExpect(jsonPath("$.itens[0].apresentacaoId").value(nullValue()));
    }

    @Test
    void deveRecusarCorrecaoComCampoMaiorQueOLimite() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Correcao longa");
        String id = importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "8101 " + nomeUnico("LINHACA") + " KG 9 8");
        Integer item = itemDaPosicao(id, 0);

        corrigir(id, item, "8101", "X".repeat(301), "KG", "9")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("descricao"));
    }

    @Test
    void deveResponderConflitoAoCorrigirImportacaoForaDeConferencia() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Correcao falha");
        String falhou = JsonPath.read(
                mockMvc.perform(multipart("/api/importacoes")
                                .file(new MockMultipartFile(
                                        "arquivo",
                                        "quebrado.pdf",
                                        "application/pdf",
                                        ("%PDF-1.4\n" + UUID.randomUUID()).getBytes(StandardCharsets.US_ASCII)))
                                .param("fornecedorId", fornecedorId.toString()))
                        .andExpect(status().isUnprocessableContent())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.importacaoId");

        corrigir(falhou, 1, "1", "ITEM", "UN", "10")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("status"));
    }

    @Test
    void deveResponderNaoEncontradoParaItemDeOutraImportacao() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Correcao outra");
        String primeira =
                importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "9001 " + nomeUnico("CACAU") + " KG 50 48");
        String segunda =
                importar(fornecedorId, PdfDeTeste.CABECALHO_REFERENCIA, "9002 " + nomeUnico("CANELA") + " KG 30 28");

        corrigir(segunda, itemDaPosicao(primeira, 0), "9001", "CACAU", "KG", "50")
                .andExpect(status().isNotFound());
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

    private Integer itemDaPosicao(String importacaoId, int posicao) throws Exception {
        String resposta = mockMvc.perform(get("/api/importacoes/{id}", importacaoId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.itens[" + posicao + "].id");
    }

    private ResultActions corrigir(
            String importacaoId, Integer itemId, String referencia, String descricao, String unidade, String preco)
            throws Exception {
        String corpo = "{\"referencia\":\"%s\",\"descricao\":\"%s\",\"unidade\":\"%s\",\"preco\":%s}"
                .formatted(referencia, descricao, unidade, preco);
        return mockMvc.perform(put("/api/importacoes/{id}/itens/{itemId}", importacaoId, itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private static String nomeUnico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
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
