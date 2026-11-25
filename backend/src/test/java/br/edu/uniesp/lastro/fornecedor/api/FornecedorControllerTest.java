package br.edu.uniesp.lastro.fornecedor.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FornecedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarFornecedorAtivoComCnpjSoDigitosEConsultarDepois() throws Exception {
        String cnpj = cnpjUnico();
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Natural Sul", formatado(cnpj), "TABELA_REFERENCIA", null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");

        mockMvc.perform(get("/api/fornecedores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Natural Sul"))
                .andExpect(jsonPath("$.cnpj").value(cnpj))
                .andExpect(jsonPath("$.contato").value("Maria (83) 99999-0000"))
                .andExpect(jsonPath("$.layoutPdf").value("TABELA_REFERENCIA"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveGuardarCnpjVazioComoNulo() throws Exception {
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Sem CNPJ", "", "ATACADO_VAREJO", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").doesNotExist());

        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outro sem CNPJ", "  ", "ATACADO_VAREJO", null)))
                .andExpect(status().isCreated());
    }

    @Test
    void deveListarComNomeLayoutEStatus() throws Exception {
        Integer id = JsonPath.read(cadastrar("Zeta Naturais", null, "ATACADO_VAREJO"), "$.id");
        mockMvc.perform(put("/api/fornecedores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Zeta Naturais", null, "ATACADO_VAREJO", false)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/fornecedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == %d)].nome", id).value(contains("Zeta Naturais")))
                .andExpect(jsonPath("$[?(@.id == %d)].layoutPdf", id).value(contains("ATACADO_VAREJO")))
                .andExpect(jsonPath("$[?(@.id == %d)].ativo", id).value(contains(false)));
    }

    @Test
    void deveEditarEInativarFornecedorMantendoDataDeCriacao() throws Exception {
        String resposta = cadastrar("Natural Sul", cnpjUnico(), "TABELA_REFERENCIA");
        Integer id = JsonPath.read(resposta, "$.id");
        String criadoEm = JsonPath.read(resposta, "$.criadoEm");
        String atualizadoEm = JsonPath.read(resposta, "$.atualizadoEm");

        mockMvc.perform(put("/api/fornecedores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Natural Sul Ltda", "", "ATACADO_VAREJO", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Natural Sul Ltda"))
                .andExpect(jsonPath("$.cnpj").doesNotExist())
                .andExpect(jsonPath("$.layoutPdf").value("ATACADO_VAREJO"))
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.criadoEm").value(criadoEm))
                .andExpect(jsonPath("$.atualizadoEm").value(not(atualizadoEm)));

        mockMvc.perform(get("/api/fornecedores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveResponder400ComTodosOsErrosDeValidacao() throws Exception {
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": " ", "cnpj": "123", "layoutPdf": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros", hasSize(3)))
                .andExpect(jsonPath("$.erros[*].campo").value(containsInAnyOrder("nome", "cnpj", "layoutPdf")));
    }

    @Test
    void deveResponder400NaEdicaoSemNomeOuLayout() throws Exception {
        Integer id = JsonPath.read(cadastrar("Natural Sul", null, "TABELA_REFERENCIA"), "$.id");

        mockMvc.perform(put("/api/fornecedores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("", null, "XML", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(containsInAnyOrder("nome", "layoutPdf")));
    }

    @Test
    void deveResponder404QuandoFornecedorNaoExiste() throws Exception {
        mockMvc.perform(get("/api/fornecedores/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erros").isArray());

        mockMvc.perform(put("/api/fornecedores/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Natural Sul", null, "TABELA_REFERENCIA", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveResponder409QuandoCnpjJaExisteNoCadastroMesmoComOutraFormatacao() throws Exception {
        String cnpj = cnpjUnico();
        cadastrar("Natural Sul", cnpj, "TABELA_REFERENCIA");

        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outro", formatado(cnpj), "ATACADO_VAREJO", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("cnpj"));
    }

    @Test
    void deveResponder409QuandoCnpjJaExisteNaEdicao() throws Exception {
        String cnpjExistente = cnpjUnico();
        cadastrar("Natural Sul", cnpjExistente, "TABELA_REFERENCIA");
        Integer id = JsonPath.read(cadastrar("Outro", cnpjUnico(), "ATACADO_VAREJO"), "$.id");

        mockMvc.perform(put("/api/fornecedores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outro", cnpjExistente, "ATACADO_VAREJO", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("cnpj"));
    }

    @Test
    void devePermitirEditarMantendoOProprioCnpj() throws Exception {
        String cnpj = cnpjUnico();
        Integer id = JsonPath.read(cadastrar("Natural Sul", cnpj, "TABELA_REFERENCIA"), "$.id");

        mockMvc.perform(put("/api/fornecedores/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Natural Sul Ltda", formatado(cnpj), "TABELA_REFERENCIA", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(cnpj));
    }

    private String cadastrar(String nome, String cnpj, String layoutPdf) throws Exception {
        return mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(nome, cnpj, layoutPdf, null)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String corpo(String nome, String cnpj, String layoutPdf, Boolean ativo) {
        String campoCnpj = cnpj == null ? "null" : "\"" + cnpj + "\"";
        return """
                {
                  "nome": "%s",
                  "cnpj": %s,
                  "contato": "Maria (83) 99999-0000",
                  "layoutPdf": "%s",
                  "ativo": %s
                }
                """.formatted(nome, campoCnpj, layoutPdf, ativo);
    }

    private static String cnpjUnico() {
        return "%014d".formatted(ThreadLocalRandom.current().nextLong(1L, 99_999_999_999_999L));
    }

    private static String formatado(String cnpj) {
        return "%s.%s.%s/%s-%s"
                .formatted(
                        cnpj.substring(0, 2),
                        cnpj.substring(2, 5),
                        cnpj.substring(5, 8),
                        cnpj.substring(8, 12),
                        cnpj.substring(12));
    }
}
