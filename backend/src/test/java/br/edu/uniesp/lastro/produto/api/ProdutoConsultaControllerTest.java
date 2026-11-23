package br.edu.uniesp.lastro.produto.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
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
class ProdutoConsultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveBuscarPorParteDoNomeSemDiferenciarMaiusculas() throws Exception {
        String marca = marcaUnica();
        cadastrar("Granola " + marca + " tradicional", "Cereais", "ATIVO");
        cadastrar("granola " + marca + " light", "Cereais", "ATIVO");
        cadastrar("Aveia " + marca, "Cereais", "ATIVO");

        mockMvc.perform(get("/api/produtos").param("nome", "GRANOLA " + marca.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[*].nome")
                        .value(contains("granola " + marca + " light", "Granola " + marca + " tradicional")));
    }

    @Test
    void deveCombinarFiltrosDeNomeCategoriaEStatus() throws Exception {
        String categoria = marcaUnica();
        cadastrar("Granola ativa", categoria, "ATIVO");
        cadastrar("Granola inativa", categoria, "INATIVO");
        cadastrar("Aveia inativa", categoria, "INATIVO");
        cadastrar("Granola inativa", "Outra " + categoria, "INATIVO");

        mockMvc.perform(get("/api/produtos")
                        .param("nome", "granola")
                        .param("categoria", " " + categoria.toLowerCase() + " ")
                        .param("status", "INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Granola inativa"))
                .andExpect(jsonPath("$.itens[0].categoria").value(categoria))
                .andExpect(jsonPath("$.itens[0].status").value("INATIVO"));
    }

    @Test
    void deveFiltrarSoPorStatus() throws Exception {
        String categoria = marcaUnica();
        cadastrar("Castanha", categoria, "ATIVO");
        cadastrar("Amendoim", categoria, "INATIVO");

        mockMvc.perform(get("/api/produtos").param("categoria", categoria).param("status", "ATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Castanha")));
    }

    @Test
    void deveTratarCuringasDoNomeComoTextoComum() throws Exception {
        String categoria = marcaUnica();
        cadastrar("Farinha 100% integral", categoria, "ATIVO");
        cadastrar("Farinha branca", categoria, "ATIVO");

        mockMvc.perform(get("/api/produtos").param("categoria", categoria).param("nome", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Farinha 100% integral")));
    }

    @Test
    void devePaginarDeVinteEmVinteOrdenadoPorNome() throws Exception {
        String categoria = marcaUnica();
        for (int numero = 21; numero >= 1; numero--) {
            cadastrar("Produto %02d".formatted(numero), categoria, "ATIVO");
        }

        mockMvc.perform(get("/api/produtos").param("categoria", categoria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(20)))
                .andExpect(jsonPath("$.itens[0].nome").value("Produto 01"))
                .andExpect(jsonPath("$.itens[19].nome").value("Produto 20"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalItens").value(21))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        mockMvc.perform(get("/api/produtos").param("categoria", categoria).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Produto 21")))
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void deveResponderListaVaziaQuandoNadaCorresponde() throws Exception {
        mockMvc.perform(get("/api/produtos").param("nome", marcaUnica()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void deveResponder400QuandoStatusInvalido() throws Exception {
        mockMvc.perform(get("/api/produtos").param("status", "SUMIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("status"));
    }

    @Test
    void deveResponder400QuandoPaginaNegativa() throws Exception {
        mockMvc.perform(get("/api/produtos").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("page"));
    }

    private void cadastrar(String nome, String categoria, String statusProduto) throws Exception {
        String corpo = """
                {
                  "nome": "%s",
                  "categoria": "%s",
                  "precoVenda": 10,
                  "status": "%s"
                }
                """.formatted(nome, categoria, statusProduto);
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated());
    }

    private static String marcaUnica() {
        return "M" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
