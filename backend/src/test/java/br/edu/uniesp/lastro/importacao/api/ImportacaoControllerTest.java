package br.edu.uniesp.lastro.importacao.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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
class ImportacaoControllerTest {

    private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRegistrarImportacaoRecebidaComNomeTamanhoHashEData() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Natural Sul", true);
        byte[] conteudo = pdfUnico();

        enviar(fornecedorId, new MockMultipartFile("arquivo", "Fornecedor_1.pdf", "application/pdf", conteudo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("^/api/importacoes/[0-9a-f-]{36}$")))
                .andExpect(jsonPath("$.id").value(matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.fornecedorId").value(fornecedorId))
                .andExpect(jsonPath("$.fornecedorNome").value("Natural Sul"))
                .andExpect(jsonPath("$.nomeArquivo").value("Fornecedor_1.pdf"))
                .andExpect(jsonPath("$.tamanhoBytes").value(conteudo.length))
                .andExpect(jsonPath("$.hashSha256").value(sha256(conteudo)))
                .andExpect(jsonPath("$.recebidaEm").isNotEmpty())
                .andExpect(jsonPath("$.status").value("EM_CONFERENCIA"))
                .andExpect(jsonPath("$.totalItens").value(2));
    }

    @Test
    void deveRecusarArquivoQueNaoEPdfMesmoComNomeEContentTypeDePdf() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Tipo Errado", true);
        byte[] planilha = "ref;produto;preco\n1076;ACUCAR;32".getBytes(StandardCharsets.UTF_8);

        enviar(fornecedorId, new MockMultipartFile("arquivo", "tabela.pdf", "application/pdf", planilha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("arquivo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("O arquivo deve ser um PDF"));
    }

    @Test
    void deveRecusarPdfAcimaDeCincoMegabytes() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Arquivo Grande", true);
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, grande, 0, 5);

        enviar(fornecedorId, new MockMultipartFile("arquivo", "grande.pdf", "application/pdf", grande))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("arquivo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("O arquivo deve ter no máximo 5 MB"));
    }

    @Test
    void deveExigirFornecedorEArquivo() throws Exception {
        mockMvc.perform(multipart("/api/importacoes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", contains("fornecedorId", "arquivo")));
    }

    @Test
    void deveResponderNaoEncontradoParaFornecedorInexistente() throws Exception {
        enviar(999_999, pdf("tabela.pdf")).andExpect(status().isNotFound());
    }

    @Test
    void deveRecusarFornecedorInativo() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Fornecedor Parado", false);

        enviar(fornecedorId, pdf("tabela.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("fornecedorId"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Fornecedor inativo não pode receber importações"));
    }

    @Test
    void deveListarImportacoesDoFornecedorDaMaisRecenteParaAMaisAntiga() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Lista Ordenada", true);
        String primeira = idDe(enviar(fornecedorId, pdf("primeira.pdf")));
        String segunda = idDe(enviar(fornecedorId, pdf("segunda.pdf")));

        mockMvc.perform(get("/api/importacoes").param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", contains(segunda, primeira)))
                .andExpect(jsonPath("$[*].nomeArquivo", contains("segunda.pdf", "primeira.pdf")))
                .andExpect(jsonPath("$[*].fornecedorNome", everyItem(is("Lista Ordenada"))));
    }

    @Test
    void deveFiltrarImportacoesPorStatus() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("Filtro Status", true);
        enviar(fornecedorId, pdf("recebida.pdf"));

        mockMvc.perform(get("/api/importacoes")
                        .param("fornecedorId", fornecedorId.toString())
                        .param("status", "EM_CONFERENCIA"))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/importacoes")
                        .param("fornecedorId", fornecedorId.toString())
                        .param("status", "CONCLUIDA"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveRecusarStatusDesconhecidoNoFiltro() throws Exception {
        mockMvc.perform(get("/api/importacoes").param("status", "QUALQUER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("status"));
    }

    private ResultActions enviar(Integer fornecedorId, MockMultipartFile arquivo) throws Exception {
        return mockMvc.perform(
                multipart("/api/importacoes").file(arquivo).param("fornecedorId", fornecedorId.toString()));
    }

    private static MockMultipartFile pdf(String nome) {
        return new MockMultipartFile("arquivo", nome, "application/pdf", pdfUnico());
    }

    private static byte[] pdfUnico() {
        return PdfDeTeste.tabelaReferenciaUnica();
    }

    private static String idDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private Integer cadastrarFornecedor(String nome, boolean ativo) throws Exception {
        String corpo = "{\"nome\":\"%s\",\"layoutPdf\":\"TABELA_REFERENCIA\",\"ativo\":true}".formatted(nome);
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");
        if (!ativo) {
            mockMvc.perform(put("/api/fornecedores/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo.replace("\"ativo\":true", "\"ativo\":false")))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private static String sha256(byte[] conteudo) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
    }
}
