export type StatusImportacao =
  | 'RECEBIDA'
  | 'EM_CONFERENCIA'
  | 'CONCLUIDA'
  | 'CONCLUIDA_COM_PENDENCIAS'
  | 'FALHOU'
  | 'DESCARTADA';

export type TomStatus = 'neutro' | 'andamento' | 'sucesso' | 'atencao' | 'erro';

export interface ApresentacaoStatus {
  rotulo: string;
  icone: string;
  tom: TomStatus;
}

export const STATUS_IMPORTACAO: Record<StatusImportacao, ApresentacaoStatus> = {
  RECEBIDA: { rotulo: 'Recebida', icone: 'inbox', tom: 'neutro' },
  EM_CONFERENCIA: { rotulo: 'Em conferência', icone: 'fact_check', tom: 'andamento' },
  CONCLUIDA: { rotulo: 'Concluída', icone: 'check_circle', tom: 'sucesso' },
  CONCLUIDA_COM_PENDENCIAS: { rotulo: 'Concluída com pendências', icone: 'rule', tom: 'atencao' },
  FALHOU: { rotulo: 'Falhou', icone: 'error', tom: 'erro' },
  DESCARTADA: { rotulo: 'Descartada', icone: 'block', tom: 'neutro' },
};

export type MotivoFalha = 'LAYOUT_NAO_RECONHECIDO' | 'PDF_ILEGIVEL' | 'ERRO_INTERNO';

export const MOTIVOS_FALHA: Record<MotivoFalha, string> = {
  LAYOUT_NAO_RECONHECIDO: 'Layout do PDF não reconhecido',
  PDF_ILEGIVEL: 'PDF ilegível',
  ERRO_INTERNO: 'Erro interno na leitura',
};

export interface Importacao {
  id: string;
  fornecedorId: number;
  fornecedorNome: string | null;
  nomeArquivo: string;
  tamanhoBytes: number;
  hashSha256: string;
  recebidaEm: string;
  concluidaEm: string | null;
  status: StatusImportacao;
  totalItens: number;
  itensValidos: number;
  itensPendentes: number;
  motivoFalha: MotivoFalha | null;
}

export interface AcaoImportacao {
  rotulo: string;
  rota: string;
}

export function acaoDaImportacao(importacao: Importacao): AcaoImportacao | null {
  switch (importacao.status) {
    case 'RECEBIDA':
    case 'EM_CONFERENCIA':
      return { rotulo: 'Conferir', rota: `/importacoes/${importacao.id}/conferencia` };
    case 'CONCLUIDA':
    case 'CONCLUIDA_COM_PENDENCIAS':
      return { rotulo: 'Ver comparação', rota: `/importacoes/${importacao.id}/comparacao` };
    default:
      return null;
  }
}
