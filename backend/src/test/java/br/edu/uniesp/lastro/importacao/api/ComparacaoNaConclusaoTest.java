package br.edu.uniesp.lastro.importacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.importacao.aplicacao.ComparacaoNaConclusao;
import br.edu.uniesp.lastro.importacao.dominio.Importacao;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.precificacao.infraestrutura.HistoricoPrecoRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Import({TestcontainersConfiguration.class, ComparacaoNaConclusaoTest.Configuracao.class})
@SpringBootTest
@AutoConfigureMockMvc
class ComparacaoNaConclusaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoricoPrecoRepository historicos;

    @Autowired
    private ComparacaoGravada comparacao;

    @BeforeEach
    void limpar() {
        comparacao.chamadas.clear();
        comparacao.falhar = false;
    }

    @Test
    void deveCompararAntesDeGravarOHistoricoDaPropriaImportacao() throws Exception {
        String nome = nomeUnico("Pistache");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();
        String linha = "3001 " + nome.toUpperCase(Locale.ROOT) + " KG %s %s";
        String anterior = importar(fornecedorId, linha.formatted("90", "88"));
        mockMvc.perform(post("/api/importacoes/{id}/concluir", anterior)).andExpect(status().isOk());
        String atual = importar(fornecedorId, linha.formatted("95", "0"));
        comparacao.chamadas.clear();

        mockMvc.perform(post("/api/importacoes/{id}/concluir", atual)).andExpect(status().isOk());

        assertThat(comparacao.chamadas).singleElement().satisfies(chamada -> {
            assertThat(chamada.importacaoId()).isEqualTo(UUID.fromString(atual));
            assertThat(chamada.totalItens()).isEqualTo(2);
            assertThat(chamada.historicoDaImportacao()).isZero();
            assertThat(chamada.ultimoPrecoAnterior()).isEqualTo("90.00");
        });
        assertThat(contarHistorico(atual)).isEqualTo(1);
    }

    @Test
    void deveDesfazerTudoQuandoAComparacaoFalha() throws Exception {
        String nome = nomeUnico("Macadamia");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();
        String id = importar(fornecedorId, "3101 " + nome.toUpperCase(Locale.ROOT) + " KG 120 118");
        comparacao.falhar = true;

        mockMvc.perform(post("/api/importacoes/{id}/concluir", id)).andExpect(status().isInternalServerError());

        assertThat(contarHistorico(id)).isZero();
        mockMvc.perform(get("/api/importacoes/{id}", id))
                .andExpect(jsonPath("$.importacao.status").value("EM_CONFERENCIA"));
    }

    private long contarHistorico(String importacaoId) {
        UUID id = UUID.fromString(importacaoId);
        return historicos.findAll().stream()
                .filter(historico -> historico.getImportacaoId().equals(id))
                .count();
    }

    private String importar(Integer fornecedorId, String linha) throws Exception {
        String resposta = mockMvc.perform(multipart("/api/importacoes")
                        .file(new MockMultipartFile(
                                "arquivo",
                                "tabela.pdf",
                                "application/pdf",
                                PdfDeTeste.comLinhas(PdfDeTeste.CABECALHO_REFERENCIA, linha)))
                        .param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private static String nomeUnico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private void cadastrarProduto(String nome) throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"precoVenda\":10}".formatted(nome)))
                .andExpect(status().isCreated());
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

    record Chamada(UUID importacaoId, int totalItens, long historicoDaImportacao, String ultimoPrecoAnterior) {}

    static class ComparacaoGravada implements ComparacaoNaConclusao {

        private final HistoricoPrecoRepository historicos;
        private final List<Chamada> chamadas = new ArrayList<>();
        private boolean falhar;

        ComparacaoGravada(HistoricoPrecoRepository historicos) {
            this.historicos = historicos;
        }

        @Override
        public void compararAntesDoHistorico(Importacao importacao, List<ItemImportado> itens) {
            if (falhar) {
                throw new IllegalStateException("Comparação indisponível");
            }
            long daImportacao = historicos.findAll().stream()
                    .filter(historico -> historico.getImportacaoId().equals(importacao.getId()))
                    .count();
            String anterior = itens.stream()
                    .filter(item -> item.getSituacao() == SituacaoItem.VALIDO)
                    .findFirst()
                    .flatMap(item -> historicos.buscarUltimoPrecoValido(item.getApresentacaoId(), importacao.getId()))
                    .map(historico -> historico.getPreco().toPlainString())
                    .orElse(null);
            chamadas.add(new Chamada(importacao.getId(), itens.size(), daImportacao, anterior));
        }
    }

    @TestConfiguration
    static class Configuracao {

        @Bean
        ComparacaoGravada comparacaoGravada(HistoricoPrecoRepository historicos) {
            return new ComparacaoGravada(historicos);
        }
    }
}
