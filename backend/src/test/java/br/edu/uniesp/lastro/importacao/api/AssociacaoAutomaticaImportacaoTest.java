package br.edu.uniesp.lastro.importacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.uniesp.lastro.TestcontainersConfiguration;
import br.edu.uniesp.lastro.importacao.PdfDeTeste;
import br.edu.uniesp.lastro.importacao.dominio.ItemImportado;
import br.edu.uniesp.lastro.importacao.dominio.OrigemVinculo;
import br.edu.uniesp.lastro.importacao.dominio.SituacaoItem;
import br.edu.uniesp.lastro.importacao.dominio.VinculoProduto;
import br.edu.uniesp.lastro.importacao.infraestrutura.ItemImportadoRepository;
import br.edu.uniesp.lastro.importacao.infraestrutura.VinculoProdutoRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AssociacaoAutomaticaImportacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemImportadoRepository itens;

    @Autowired
    private VinculoProdutoRepository vinculos;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveAssociarPorNomeExatoRegistrandoVinculoAutomatico() throws Exception {
        String nome = nomeUnico("Cha de hibisco");
        Integer produtoId = cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();

        String id = idDe(enviar(
                        fornecedorId,
                        PdfDeTeste.comLinhas(
                                PdfDeTeste.CABECALHO_REFERENCIA,
                                "1078 " + nome.toUpperCase(Locale.ROOT) + " UN 20 18",
                                "1079 " + nomeUnico("PRODUTO SEM CADASTRO") + " UN 10 9"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItens").value(4))
                .andExpect(jsonPath("$.itensValidos").value(2))
                .andExpect(jsonPath("$.itensPendentes").value(2)));

        List<ItemImportado> lidos = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id));
        assertThat(lidos)
                .extracting(ItemImportado::getSituacao)
                .containsExactly(
                        SituacaoItem.VALIDO,
                        SituacaoItem.VALIDO,
                        SituacaoItem.PENDENTE_ASSOCIACAO,
                        SituacaoItem.PENDENTE_ASSOCIACAO);
        List<VinculoProduto> criados = List.of(
                vinculos.findByApresentacaoIdAndAtivoTrue(lidos.get(0).getApresentacaoId())
                        .orElseThrow(),
                vinculos.findByApresentacaoIdAndAtivoTrue(lidos.get(1).getApresentacaoId())
                        .orElseThrow());
        assertThat(criados).allSatisfy(vinculo -> {
            assertThat(vinculo.getProdutoId()).isEqualTo(produtoId.longValue());
            assertThat(vinculo.getOrigem()).isEqualTo(OrigemVinculo.AUTOMATICA);
            assertThat(vinculo.isAtivo()).isTrue();
        });
        assertThat(vinculos.findByApresentacaoIdAndAtivoTrue(lidos.get(2).getApresentacaoId()))
                .isEmpty();
    }

    @Test
    void deveUsarOVinculoSalvoNaProximaImportacaoMesmoComDescricaoDiferente() throws Exception {
        String nome = nomeUnico("Granola tradicional");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();
        enviar(
                        fornecedorId,
                        PdfDeTeste.comLinhas(
                                PdfDeTeste.CABECALHO_REFERENCIA, "2001 " + nome.toUpperCase(Locale.ROOT) + " KG 30 28"))
                .andExpect(jsonPath("$.itensValidos").value(2));

        String id = idDe(enviar(
                        fornecedorId,
                        PdfDeTeste.comLinhas(PdfDeTeste.CABECALHO_REFERENCIA, "2001 GRANOLA TRAD PACOTE NOVO KG 31 29"))
                .andExpect(jsonPath("$.itensValidos").value(2)));

        assertThat(itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id)))
                .extracting(ItemImportado::getSituacao)
                .containsOnly(SituacaoItem.VALIDO);
        assertThat(vinculosDoFornecedor(fornecedorId)).isEqualTo(2);
    }

    @Test
    void deveDeixarPendenteQuandoDoisProdutosTemOMesmoNome() throws Exception {
        String nome = nomeUnico("Aveia em flocos");
        cadastrarProduto(nome);
        cadastrarProduto(nome.toLowerCase(Locale.ROOT));
        Integer fornecedorId = cadastrarFornecedor();

        enviar(
                        fornecedorId,
                        PdfDeTeste.comLinhas(
                                PdfDeTeste.CABECALHO_REFERENCIA, "3001 " + nome.toUpperCase(Locale.ROOT) + " KG 12 11"))
                .andExpect(jsonPath("$.itensValidos").value(0))
                .andExpect(jsonPath("$.itensPendentes").value(2));

        assertThat(vinculosDoFornecedor(fornecedorId)).isZero();
    }

    @Test
    void deveDeixarPendenteQuandoADescricaoSoSeParece() throws Exception {
        String nome = nomeUnico("Castanha do para");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();

        enviar(
                        fornecedorId,
                        PdfDeTeste.comLinhas(
                                PdfDeTeste.CABECALHO_REFERENCIA,
                                "4001 " + nome.toUpperCase(Locale.ROOT) + " TORRADA KG 80 78"))
                .andExpect(jsonPath("$.itensValidos").value(0));

        assertThat(vinculosDoFornecedor(fornecedorId)).isZero();
    }

    @Test
    void deveManterInvalidoOItemAssociadoComPrecoZero() throws Exception {
        String nome = nomeUnico("Mel silvestre");
        cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();

        String id = idDe(enviar(
                fornecedorId,
                PdfDeTeste.comLinhas(
                        PdfDeTeste.CABECALHO_REFERENCIA, "5001 " + nome.toUpperCase(Locale.ROOT) + " UN 25 0")));

        assertThat(itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id)))
                .extracting(ItemImportado::getSituacao)
                .containsExactly(SituacaoItem.VALIDO, SituacaoItem.INVALIDO);
    }

    @Test
    void deveImpedirDoisVinculosAtivosParaAMesmaApresentacao() throws Exception {
        String nome = nomeUnico("Linhaca dourada");
        Integer produtoId = cadastrarProduto(nome);
        Integer fornecedorId = cadastrarFornecedor();
        String id = idDe(enviar(
                fornecedorId,
                PdfDeTeste.comLinhas(
                        PdfDeTeste.CABECALHO_REFERENCIA, "6001 " + nome.toUpperCase(Locale.ROOT) + " KG 15 14")));
        Long apresentacaoId = itens.findByImportacaoIdOrderByNumeroLinhaAscIdAsc(UUID.fromString(id))
                .getFirst()
                .getApresentacaoId();

        assertThatThrownBy(() -> vinculos.saveAndFlush(
                        VinculoProduto.automatico(apresentacaoId, produtoId.longValue(), Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("uk_vinculo_produto_apresentacao_ativo");
    }

    private ResultActions enviar(Integer fornecedorId, byte[] pdf) throws Exception {
        return mockMvc.perform(multipart("/api/importacoes")
                .file(new MockMultipartFile("arquivo", "tabela.pdf", "application/pdf", pdf))
                .param("fornecedorId", fornecedorId.toString()));
    }

    private static String idDe(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static String nomeUnico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private Integer vinculosDoFornecedor(Integer fornecedorId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM vinculo_produto v
                JOIN apresentacao_fornecedor a ON a.id = v.apresentacao_id
                WHERE a.fornecedor_id = ?
                """, Integer.class, fornecedorId);
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

    private Integer cadastrarFornecedor() throws Exception {
        String resposta = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Associacao %s\",\"layoutPdf\":\"TABELA_REFERENCIA\",\"ativo\":true}"
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }
}
