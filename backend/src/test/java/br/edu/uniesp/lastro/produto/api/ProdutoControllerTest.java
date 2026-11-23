package br.edu.uniesp.lastro.produto.api;

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
import java.util.UUID;
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
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarProdutoEConsultarDepois() throws Exception {
        String codigo = codigoUnico();
        String resposta = mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(codigo, "Granola tradicional", "18.90")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");

        mockMvc.perform(get("/api/produtos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.nome").value("Granola tradicional"))
                .andExpect(jsonPath("$.categoria").value("Cereais"))
                .andExpect(jsonPath("$.unidade").value("UN"))
                .andExpect(jsonPath("$.precoCompra").value(12.5))
                .andExpect(jsonPath("$.precoVenda").value(18.9))
                .andExpect(jsonPath("$.estoqueAtual").value(10))
                .andExpect(jsonPath("$.estoqueMinimo").value(2));
    }

    @Test
    void deveEditarProdutoEAtualizarDataDeAtualizacao() throws Exception {
        String resposta = cadastrar(codigoUnico(), "Aveia em flocos", "9.90");
        Integer id = JsonPath.read(resposta, "$.id");
        String atualizadoEmAntes = JsonPath.read(resposta, "$.atualizadoEm");
        String criadoEm = JsonPath.read(resposta, "$.criadoEm");

        mockMvc.perform(put("/api/produtos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, "Aveia em flocos finos", "10.50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Aveia em flocos finos"))
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.precoVenda").value(10.5))
                .andExpect(jsonPath("$.criadoEm").value(criadoEm))
                .andExpect(jsonPath("$.atualizadoEm").value(not(atualizadoEmAntes)));
    }

    @Test
    void deveResponder400ComTodosOsErrosDeValidacao() throws Exception {
        String corpoInvalido = """
                {"nome": " ", "precoCompra": -1, "estoqueAtual": -5, "estoqueMinimo": -1}
                """;

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros", hasSize(5)))
                .andExpect(jsonPath("$.erros[*].campo")
                        .value(containsInAnyOrder(
                                "nome", "precoCompra", "precoVenda", "estoqueAtual", "estoqueMinimo")));
    }

    @Test
    void deveResponder400QuandoValorNaoForNumero() throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Granola", "precoVenda": "dez reais"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("precoVenda"));
    }

    @Test
    void deveResponder404QuandoProdutoNaoExiste() throws Exception {
        mockMvc.perform(get("/api/produtos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erros").isArray());

        mockMvc.perform(put("/api/produtos/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, "Granola", "10")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveResponder409QuandoCodigoJaExisteNoCadastro() throws Exception {
        String codigo = codigoUnico();
        cadastrar(codigo, "Granola", "10");

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(codigo, "Outra granola", "11")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("codigo"));
    }

    @Test
    void deveResponder409QuandoCodigoJaExisteNaEdicao() throws Exception {
        String codigoExistente = codigoUnico();
        cadastrar(codigoExistente, "Granola", "10");
        Integer id = JsonPath.read(cadastrar(codigoUnico(), "Aveia", "8"), "$.id");

        mockMvc.perform(put("/api/produtos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(codigoExistente, "Aveia", "8")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("codigo"));
    }

    @Test
    void devePermitirEditarMantendoOProprioCodigo() throws Exception {
        String codigo = codigoUnico();
        Integer id = JsonPath.read(cadastrar(codigo, "Granola", "10"), "$.id");

        mockMvc.perform(put("/api/produtos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(codigo, "Granola", "12")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoVenda").value(12));
    }

    private String cadastrar(String codigo, String nome, String precoVenda) throws Exception {
        return mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(codigo, nome, precoVenda)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String corpo(String codigo, String nome, String precoVenda) {
        String campoCodigo = codigo == null ? "null" : "\"" + codigo + "\"";
        return """
                {
                  "codigo": %s,
                  "nome": "%s",
                  "categoria": "Cereais",
                  "unidade": "UN",
                  "precoCompra": 12.50,
                  "precoVenda": %s,
                  "estoqueAtual": 10,
                  "estoqueMinimo": 2
                }
                """.formatted(campoCodigo, nome, precoVenda);
    }

    private static String codigoUnico() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
