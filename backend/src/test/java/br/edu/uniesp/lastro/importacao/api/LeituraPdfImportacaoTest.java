package br.edu.uniesp.lastro.importacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.ModalidadePreco;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.infraestrutura.ItemImportadoRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
class LeituraPdfImportacaoTest {

    private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemImportadoRepository itens;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveLerOPdfDeReferenciaEDeixarAImportacaoEmConferencia() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");

        String id = idDe(enviar(fornecedorId, "Fornecedor_1.pdf", PdfDeTeste.recurso("Fornecedor_1.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EM_CONFERENCIA"))
                .andExpect(jsonPath("$.totalItens").value(4))
                .andExpect(jsonPath("$.itensValidos").value(0))
                .andExpect(jsonPath("$.itensPendentes").value(4)));

        List<ItemImportado> lidos = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id));
        assertThat(lidos)
                .extracting(
                        ItemImportado::getNumeroLinha,
                        ItemImportado::getReferencia,
                        ItemImportado::getDescricao,
                        ItemImportado::getUnidade,
                        ItemImportado::getModalidadePreco,
                        ItemImportado::getPreco,
                        ItemImportado::getSituacao)
                .containsExactly(
                        tuple(
                                2,
                                "1076",
                                "ACUCAR DE COCO",
                                "KG",
                                ModalidadePreco.UNITARIO,
                                new BigDecimal("32.00"),
                                SituacaoItem.PENDENTE_ASSOCIACAO),
                        tuple(
                                2,
                                "1076",
                                "ACUCAR DE COCO",
                                "KG",
                                ModalidadePreco.FARDO,
                                new BigDecimal("32.00"),
                                SituacaoItem.PENDENTE_ASSOCIACAO),
                        tuple(
                                3,
                                "1077",
                                "DOCANTE STEVIA COLOR ANDINA 40G",
                                "UN",
                                ModalidadePreco.UNITARIO,
                                new BigDecimal("78.00"),
                                SituacaoItem.PENDENTE_ASSOCIACAO),
                        tuple(
                                3,
                                "1077",
                                "DOCANTE STEVIA COLOR ANDINA 40G",
                                "UN",
                                ModalidadePreco.FARDO,
                                new BigDecimal("78.00"),
                                SituacaoItem.PENDENTE_ASSOCIACAO));
        assertThat(lidos)
                .allSatisfy(item -> assertThat(item.getApresentacaoId()).isNotNull());
        assertThat(apresentacoesDo(fornecedorId)).isEqualTo(4);
    }

    @Test
    void deveReaproveitarAsApresentacoesJaConhecidasDoFornecedor() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        enviar(fornecedorId, "Fornecedor_1.pdf", PdfDeTeste.recurso("Fornecedor_1.pdf"))
                .andExpect(status().isCreated());

        enviar(
                        fornecedorId,
                        "Fornecedor_1_v2.pdf",
                        PdfDeTeste.comLinhas(
                                PdfDeTeste.CABECALHO_REFERENCIA,
                                "1076 ACUCAR DE COCO KG 35 32",
                                "1078 CHA DE HIBISCO 100G UN 20 18"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItens").value(4));

        assertThat(apresentacoesDo(fornecedorId)).isEqualTo(6);
    }

    @Test
    void deveGuardarItemInvalidoComALinhaOriginalEOsErros() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] pdf = PdfDeTeste.comLinhas(
                PdfDeTeste.CABECALHO_REFERENCIA,
                "1077 DOCANTE STEVIA COLOR ANDINA 40G UN 74 0",
                "Frete por conta do cliente");

        String id = idDe(enviar(fornecedorId, "com_erros.pdf", pdf)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItens").value(3)));

        List<ItemImportado> lidos = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id));
        assertThat(lidos)
                .extracting(ItemImportado::getSituacao)
                .containsExactly(SituacaoItem.PENDENTE_ASSOCIACAO, SituacaoItem.INVALIDO, SituacaoItem.INVALIDO);
        assertThat(lidos.get(1).getErros()).extracting(erro -> erro.codigo()).containsExactly("PRECO_ZERO");
        assertThat(lidos.get(2).getLinhaOriginal()).isEqualTo("Frete por conta do cliente");
        assertThat(lidos.get(2).getErros().getFirst().codigo()).isEqualTo("LINHA_FORA_DO_PADRAO");
        assertThat(lidos.get(2).getApresentacaoId()).isNull();
    }

    @Test
    void deveRegistrarFalhaDeLayoutEResponder422ComOId() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");

        String id = JsonPath.read(
                enviar(fornecedorId, "Fornecedor_2.pdf", PdfDeTeste.recurso("Fornecedor_2.pdf"))
                        .andExpect(status().isUnprocessableContent())
                        .andExpect(jsonPath("$.importacaoId").value(matchesPattern(UUID_PATTERN)))
                        .andExpect(jsonPath("$.motivoFalha").value("LAYOUT_NAO_RECONHECIDO"))
                        .andExpect(jsonPath("$.erros[0].campo").value("arquivo"))
                        .andExpect(jsonPath("$.erros[0].mensagem")
                                .value("O PDF não segue o layout cadastrado para o fornecedor"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.importacaoId");

        mockMvc.perform(get("/api/importacoes").param("fornecedorId", fornecedorId.toString()))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("FALHOU"))
                .andExpect(jsonPath("$[0].motivoFalha").value("LAYOUT_NAO_RECONHECIDO"))
                .andExpect(jsonPath("$[0].totalItens").value(0));
        assertThat(itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id)))
                .isEmpty();
    }

    @Test
    void deveRegistrarFalhaDePdfIlegivel() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] quebrado = ("%PDF-1.4\n" + UUID.randomUUID()).getBytes(StandardCharsets.US_ASCII);

        enviar(fornecedorId, "quebrado.pdf", quebrado)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.motivoFalha").value("PDF_ILEGIVEL"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Não foi possível ler o texto do PDF"));
    }

    @Test
    void deveDeixarRecebidaAImportacaoDeLayoutAindaSemLeitor() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("ATACADO_VAREJO");

        enviar(fornecedorId, "Fornecedor_2.pdf", PdfDeTeste.recurso("Fornecedor_2.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"));
    }

    private ResultActions enviar(Integer fornecedorId, String nome, byte[] conteudo) throws Exception {
        return mockMvc.perform(multipart("/api/importacoes")
                .file(new MockMultipartFile("arquivo", nome, "application/pdf", conteudo))
                .param("fornecedorId", fornecedorId.toString()));
    }

    private static String idDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private Integer apresentacoesDo(Integer fornecedorId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM apresentacao_fornecedor WHERE fornecedor_id = ?", Integer.class, fornecedorId);
    }

    private Integer cadastrarFornecedor(String layout) throws Exception {
        String corpo =
                "{\"nome\":\"Leitura %s\",\"layoutPdf\":\"%s\",\"ativo\":true}".formatted(UUID.randomUUID(), layout);
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }
}
