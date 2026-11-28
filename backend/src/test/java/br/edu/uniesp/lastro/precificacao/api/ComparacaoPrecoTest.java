package br.edu.uniesp.lastro.precificacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.precificacao.dominio.Classificacao;
import br.edu.uniesp.lastro.precificacao.dominio.ComparacaoPreco;
import br.edu.uniesp.lastro.precificacao.infraestrutura.ComparacaoPrecoRepository;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ComparacaoPrecoTest {

    private static final String COMPARACAO = "/api/importacoes/{id}/comparacao";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComparacaoPrecoRepository comparacoes;

    @Test
    void deveUsarUltimoPrecoValidoMesmoQuandoAImportacaoAnteriorNaoTrouxeOItem() throws Exception {
        String hibisco = cadastrarProduto("Hibisco", "50.00");
        Integer fornecedorId = cadastrarFornecedor();
        concluir(importar(fornecedorId, linha("4001", hibisco, "33 30")));
        concluir(importar(fornecedorId, linha("4002", semProduto(), "10 9")));
        String terceira = importar(fornecedorId, linha("4001", hibisco, "35 30"));

        concluir(terceira);

        mockMvc.perform(get(COMPARACAO, terceira).param("classificacao", "AUMENTOU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].modalidadePreco").value("UNITARIO"))
                .andExpect(jsonPath("$.itens[0].precoAnterior").value(33.00))
                .andExpect(jsonPath("$.itens[0].precoAtual").value(35.00))
                .andExpect(jsonPath("$.itens[0].diferenca").value(2.00))
                .andExpect(jsonPath("$.itens[0].variacaoPercentual").value(6.06))
                .andExpect(jsonPath("$.itens[0].precoVenda").value(50.00))
                .andExpect(jsonPath("$.itens[0].produtoNome").value(hibisco));
    }

    @Test
    void deveClassificarTodosOsItensEGravarAComparacaoInclusiveDosPendentes() throws Exception {
        String hibisco = cadastrarProduto("Hibisco", "50.00");
        String camomila = cadastrarProduto("Camomila", "65.00");
        String novo = cadastrarProduto("Produto novo", "20.00");
        Integer fornecedorId = cadastrarFornecedor();
        concluir(importar(fornecedorId, linha("5001", hibisco, "33 15"), linha("5002", camomila, "38 10")));
        String atual = importar(
                fornecedorId,
                linha("5001", hibisco, "35 14"),
                linha("5002", camomila, "38 0"),
                linha("5003", novo, "20 18"),
                linha("5004", semProduto(), "20 18"));

        concluir(atual);

        mockMvc.perform(get(COMPARACAO, atual))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importacao.id").value(atual))
                .andExpect(jsonPath("$.importacao.status").value("CONCLUIDA_COM_PENDENCIAS"))
                .andExpect(jsonPath("$.itens", hasSize(8)))
                .andExpect(jsonPath("$.totais.AUMENTOU").value(1))
                .andExpect(jsonPath("$.totais.DIMINUIU").value(1))
                .andExpect(jsonPath("$.totais.SEM_ALTERACAO").value(1))
                .andExpect(jsonPath("$.totais.NOVO").value(2))
                .andExpect(jsonPath("$.totais.PENDENTE").value(3))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'AUMENTOU')].variacaoPercentual", contains(6.06)))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'DIMINUIU')].precoAnterior", contains(15.0)))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'DIMINUIU')].diferenca", contains(-1.0)))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'DIMINUIU')].variacaoPercentual", contains(-6.67)))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'SEM_ALTERACAO')].variacaoPercentual", contains(0.0)))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'NOVO')].precoAnterior", everyItem(nullValue())))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'NOVO')].variacaoPercentual", everyItem(nullValue())))
                .andExpect(jsonPath("$.itens[?(@.classificacao == 'NOVO')].precoVenda", contains(20.0, 20.0)));

        mockMvc.perform(get(COMPARACAO, atual).param("classificacao", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.totais.AUMENTOU").value(1))
                .andExpect(jsonPath("$.itens[0].numeroLinha").value(3))
                .andExpect(jsonPath("$.itens[0].precoAnterior").value(10.00))
                .andExpect(jsonPath("$.itens[0].precoAtual").value(0.00))
                .andExpect(jsonPath("$.itens[0].diferenca").value(nullValue()))
                .andExpect(jsonPath("$.itens[0].motivosPendencia", contains("Preço igual a zero")))
                .andExpect(jsonPath("$.itens[1].produtoNome").value(nullValue()))
                .andExpect(jsonPath("$.itens[1].motivosPendencia", contains("Item sem produto associado")));

        List<ComparacaoPreco> gravadas = comparacoes.findByImportacaoId(UUID.fromString(atual));
        assertThat(gravadas).hasSize(8);
        assertThat(gravadas)
                .filteredOn(comparacao -> comparacao.getClassificacao() == Classificacao.PENDENTE)
                .hasSize(3);
    }

    @Test
    void naoDeveCompararComOPrecoDaPropriaImportacao() throws Exception {
        String alecrim = cadastrarProduto("Alecrim", "35.00");
        Integer fornecedorId = cadastrarFornecedor();
        String primeira = importar(fornecedorId, linha("6001", alecrim, "15 14"));

        concluir(primeira);

        mockMvc.perform(get(COMPARACAO, primeira))
                .andExpect(jsonPath("$.totais.NOVO").value(2))
                .andExpect(jsonPath("$.itens[*].precoAnterior", everyItem(nullValue())));
    }

    @Test
    void deveResponderNaoEncontradoParaImportacaoInexistente() throws Exception {
        mockMvc.perform(get(COMPARACAO, UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void deveRecusarComparacaoDeImportacaoAindaEmConferencia() throws Exception {
        Integer fornecedorId = cadastrarFornecedor();
        String id = importar(fornecedorId, linha("7001", semProduto(), "10 9"));

        mockMvc.perform(get(COMPARACAO, id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erros[0].campo").value("status"));
    }

    @Test
    void deveRecusarClassificacaoDesconhecidaNoFiltro() throws Exception {
        mockMvc.perform(get(COMPARACAO, UUID.randomUUID()).param("classificacao", "SUBIU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("classificacao"));
    }

    private void concluir(String importacaoId) throws Exception {
        mockMvc.perform(post("/api/importacoes/{id}/concluir", importacaoId)).andExpect(status().isOk());
    }

    private String importar(Integer fornecedorId, String... linhas) throws Exception {
        String[] conteudo = Stream.concat(Stream.of(PdfDeTeste.CABECALHO_REFERENCIA), Stream.of(linhas))
                .toArray(String[]::new);
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

    private String cadastrarProduto(String base, String precoVenda) throws Exception {
        String nome = base + " " + sufixo();
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"precoVenda\":%s}".formatted(nome, precoVenda)))
                .andExpect(status().isCreated());
        return nome;
    }

    private static String linha(String referencia, String descricao, String precos) {
        return referencia + " " + descricao.toUpperCase(Locale.ROOT) + " KG " + precos;
    }

    private static String semProduto() {
        return "SEM PRODUTO " + sufixo().toUpperCase(Locale.ROOT);
    }

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Integer cadastrarFornecedor() throws Exception {
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Comparacao %s\",\"layoutPdf\":\"TABELA_REFERENCIA\",\"ativo\":true}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }
}
