package br.edu.uniesp.lastro.importacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.infraestrutura.ImportacaoRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReimportacaoDuplicadaTest {

    private static final String MENSAGEM_DUPLICADA = "Este arquivo já foi importado para este fornecedor";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private ImportacaoRepository importacoes;

    @Test
    void deveResponderConflitoComOIdDaImportacaoOriginalSemGravarNada() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] pdf = PdfDeTeste.tabelaReferenciaUnica();
        String original = idDe(enviar(fornecedorId, "tabela.pdf", pdf).andExpect(status().isCreated()));
        int apresentacoesAntes = apresentacoesDo(fornecedorId);

        enviar(fornecedorId, "tabela_de_novo.pdf", pdf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.importacaoId").value(original))
                .andExpect(jsonPath("$.detail").value(MENSAGEM_DUPLICADA))
                .andExpect(jsonPath("$.erros[0].campo").value("arquivo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value(MENSAGEM_DUPLICADA));

        assertThat(importacoesDo(fornecedorId)).isEqualTo(1);
        assertThat(apresentacoesDo(fornecedorId)).isEqualTo(apresentacoesAntes);
    }

    @Test
    void deveContinuarBloqueandoDepoisDaImportacaoConcluida() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] pdf = PdfDeTeste.tabelaReferenciaUnica();
        String original = idDe(enviar(fornecedorId, "tabela.pdf", pdf));
        mockMvc.perform(post("/api/importacoes/{id}/concluir", original)).andExpect(status().isOk());

        enviar(fornecedorId, "tabela.pdf", pdf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.importacaoId").value(original));
    }

    @Test
    void deveAceitarOMesmoArquivoParaOutroFornecedor() throws Exception {
        byte[] pdf = PdfDeTeste.tabelaReferenciaUnica();
        enviar(cadastrarFornecedor("TABELA_REFERENCIA"), "tabela.pdf", pdf).andExpect(status().isCreated());

        enviar(cadastrarFornecedor("TABELA_REFERENCIA"), "tabela.pdf", pdf).andExpect(status().isCreated());
    }

    @Test
    void deveAceitarReenvioDepoisDoDescarte() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] pdf = PdfDeTeste.tabelaReferenciaUnica();
        String descartada = idDe(enviar(fornecedorId, "tabela.pdf", pdf));
        mockMvc.perform(post("/api/importacoes/{id}/descartar", descartada)).andExpect(status().isOk());

        String nova = idDe(enviar(fornecedorId, "tabela.pdf", pdf).andExpect(status().isCreated()));

        assertThat(nova).isNotEqualTo(descartada);
    }

    @Test
    void deveAceitarReenvioDepoisDaFalhaQuandoOLayoutEhCorrigido() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] pdf = PdfDeTeste.recurso("Fornecedor_2.pdf");
        enviar(fornecedorId, "Fornecedor_2.pdf", pdf).andExpect(status().isUnprocessableContent());

        mudarLayout(fornecedorId, "ATACADO_VAREJO");

        enviar(fornecedorId, "Fornecedor_2.pdf", pdf).andExpect(status().isCreated());
        assertThat(importacoesDo(fornecedorId)).isEqualTo(2);
    }

    @Test
    void deveResponderConflitoQuandoOutroEnvioGravaOMesmoArquivoAntes() throws Exception {
        Integer fornecedorId = cadastrarFornecedor("TABELA_REFERENCIA");
        byte[] pdf = PdfDeTeste.tabelaReferenciaUnica();
        String original = idDe(enviar(fornecedorId, "tabela.pdf", pdf));
        int apresentacoesAntes = apresentacoesDo(fornecedorId);
        Importacao gravadaPorOutroEnvio =
                importacoes.findById(UUID.fromString(original)).orElseThrow();
        doReturn(Optional.empty())
                .doReturn(Optional.of(gravadaPorOutroEnvio))
                .when(importacoes)
                .findFirstByFornecedorIdAndHashSha256AndStatusNotIn(anyLong(), anyString(), any());

        enviar(fornecedorId, "tabela.pdf", pdf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.importacaoId").value(original));

        assertThat(importacoesDo(fornecedorId)).isEqualTo(1);
        assertThat(apresentacoesDo(fornecedorId)).isEqualTo(apresentacoesAntes);
    }

    private ResultActions enviar(Integer fornecedorId, String nome, byte[] conteudo) throws Exception {
        return mockMvc.perform(multipart("/api/importacoes")
                .file(new MockMultipartFile("arquivo", nome, "application/pdf", conteudo))
                .param("fornecedorId", fornecedorId.toString()));
    }

    private static String idDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private Integer importacoesDo(Integer fornecedorId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM importacao WHERE fornecedor_id = ?", Integer.class, fornecedorId);
    }

    private Integer apresentacoesDo(Integer fornecedorId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM apresentacao_fornecedor WHERE fornecedor_id = ?", Integer.class, fornecedorId);
    }

    private Integer cadastrarFornecedor(String layout) throws Exception {
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoFornecedor(UUID.randomUUID().toString(), layout)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private void mudarLayout(Integer fornecedorId, String layout) throws Exception {
        mockMvc.perform(put("/api/fornecedores/{id}", fornecedorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoFornecedor(fornecedorId.toString(), layout)))
                .andExpect(status().isOk());
    }

    private static String corpoFornecedor(String sufixo, String layout) {
        return "{\"nome\":\"Duplicidade %s\",\"layoutPdf\":\"%s\",\"ativo\":true}".formatted(sufixo, layout);
    }
}
