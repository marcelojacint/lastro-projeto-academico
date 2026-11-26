package br.edu.uniesp.lastro.importacao.dominio;

public class FalhaLeituraException extends RuntimeException {

    private final MotivoFalha motivo;

    public FalhaLeituraException(MotivoFalha motivo, String mensagem) {
        super(mensagem);
        this.motivo = motivo;
    }

    public MotivoFalha getMotivo() {
        return motivo;
    }
}
