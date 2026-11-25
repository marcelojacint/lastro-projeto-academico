package br.edu.uniesp.lastro.importacao.dominio;

import br.edu.uniesp.lastro.compartilhado.erro.DadosInvalidosException;
import br.edu.uniesp.lastro.compartilhado.erro.ErroCampo;
import br.edu.uniesp.lastro.fornecedor.dominio.Fornecedor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public record SolicitacaoImportacao(Long fornecedorId, String nomeArquivo, long tamanhoBytes, String hashSha256) {

    public static final int TAMANHO_MAXIMO_MB = 5;
    public static final long TAMANHO_MAXIMO_BYTES = TAMANHO_MAXIMO_MB * 1024L * 1024L;
    public static final String MENSAGEM_TAMANHO_EXCEDIDO = "O arquivo deve ter no máximo " + TAMANHO_MAXIMO_MB + " MB";
    static final int TAMANHO_MAXIMO_NOME = 255;
    static final String NOME_PADRAO = "arquivo.pdf";

    private static final byte[] ASSINATURA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public static SolicitacaoImportacao validar(Long fornecedorId, String nomeOriginal, byte[] conteudo) {
        List<ErroCampo> erros = new ArrayList<>();
        if (fornecedorId == null) {
            erros.add(new ErroCampo("fornecedorId", "Fornecedor é obrigatório"));
        }
        String nome = nomeSemCaminho(nomeOriginal);
        if (conteudo == null || conteudo.length == 0) {
            erros.add(new ErroCampo("arquivo", "Arquivo é obrigatório"));
        } else {
            if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
                erros.add(new ErroCampo("arquivo", MENSAGEM_TAMANHO_EXCEDIDO));
            }
            if (!comecaComAssinaturaPdf(conteudo)) {
                erros.add(new ErroCampo("arquivo", "O arquivo deve ser um PDF"));
            }
            if (nome.length() > TAMANHO_MAXIMO_NOME) {
                erros.add(new ErroCampo(
                        "arquivo", "O nome do arquivo deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres"));
            }
        }
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
        return new SolicitacaoImportacao(fornecedorId, nome, conteudo.length, sha256(conteudo));
    }

    public void exigirFornecedorAtivo(Fornecedor fornecedor) {
        if (!fornecedor.isAtivo()) {
            throw new DadosInvalidosException(
                    List.of(new ErroCampo("fornecedorId", "Fornecedor inativo não pode receber importações")));
        }
    }

    private static String nomeSemCaminho(String nomeOriginal) {
        if (nomeOriginal == null) {
            return NOME_PADRAO;
        }
        String nome = nomeOriginal
                .substring(Math.max(nomeOriginal.lastIndexOf('/'), nomeOriginal.lastIndexOf('\\')) + 1)
                .strip();
        return nome.isEmpty() ? NOME_PADRAO : nome;
    }

    private static boolean comecaComAssinaturaPdf(byte[] conteudo) {
        return conteudo.length >= ASSINATURA_PDF.length
                && Arrays.equals(conteudo, 0, ASSINATURA_PDF.length, ASSINATURA_PDF, 0, ASSINATURA_PDF.length);
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", excecao);
        }
    }
}
