import { ApresentacaoStatus, Importacao } from './importacao';

export interface MensagemErro {
  codigo: string;
  mensagem: string;
}

export type SituacaoItem = 'VALIDO' | 'PENDENTE_ASSOCIACAO' | 'INVALIDO';

export const SITUACOES_ITEM: Record<SituacaoItem, ApresentacaoStatus> = {
  VALIDO: { rotulo: 'Válido', icone: 'check_circle', tom: 'sucesso' },
  PENDENTE_ASSOCIACAO: { rotulo: 'Pendente de associação', icone: 'link_off', tom: 'atencao' },
  INVALIDO: { rotulo: 'Inválido', icone: 'warning', tom: 'erro' },
};

export type ModalidadePreco = 'UNITARIO' | 'FARDO' | 'ATACADO' | 'VAREJO';

export const MODALIDADES_PRECO: Record<ModalidadePreco, string> = {
  UNITARIO: 'Unitário',
  FARDO: 'Fardo',
  ATACADO: 'Atacado',
  VAREJO: 'Varejo',
};

export interface ItemConferencia {
  id: number;
  numeroLinha: number;
  linhaOriginal: string;
  referencia: string | null;
  descricao: string | null;
  unidade: string | null;
  modalidadePreco: ModalidadePreco | null;
  quantidade: number | null;
  preco: number | null;
  situacao: SituacaoItem;
  erros: MensagemErro[];
  apresentacaoId: number | null;
  produtoId: number | null;
  produtoNome: string | null;
}

export interface ImportacaoDetalhe {
  importacao: Importacao;
  itens: ItemConferencia[];
}

export interface CorrecaoItem {
  referencia: string | null;
  descricao: string | null;
  unidade: string | null;
  preco: number | null;
}
