import { ApresentacaoStatus, Importacao } from './importacao';
import { ModalidadePreco } from './item-importado';

export type Classificacao = 'AUMENTOU' | 'DIMINUIU' | 'SEM_ALTERACAO' | 'NOVO' | 'PENDENTE';

export const CLASSIFICACOES: Record<Classificacao, ApresentacaoStatus> = {
  AUMENTOU: { rotulo: 'Aumentou', icone: 'arrow_upward', tom: 'erro' },
  DIMINUIU: { rotulo: 'Diminuiu', icone: 'arrow_downward', tom: 'queda' },
  SEM_ALTERACAO: { rotulo: 'Sem alteração', icone: 'equal', tom: 'neutro' },
  NOVO: { rotulo: 'Novo', icone: 'star', tom: 'andamento' },
  PENDENTE: { rotulo: 'Pendente', icone: 'warning', tom: 'pendente' },
};

export const ORDEM_CLASSIFICACOES: Classificacao[] = [
  'AUMENTOU',
  'DIMINUIU',
  'SEM_ALTERACAO',
  'NOVO',
  'PENDENTE',
];

export interface ItemComparado {
  itemId: number;
  numeroLinha: number;
  referencia: string | null;
  descricao: string | null;
  unidade: string | null;
  modalidadePreco: ModalidadePreco | null;
  produtoId: number | null;
  produtoNome: string | null;
  precoVenda: number | null;
  precoAnterior: number | null;
  precoAtual: number | null;
  diferenca: number | null;
  variacaoPercentual: number | null;
  classificacao: Classificacao;
  motivosPendencia: string[];
}

export interface ItemAusente {
  apresentacaoId: number;
  referencia: string | null;
  descricao: string;
  unidade: string | null;
  modalidadePreco: ModalidadePreco;
  produtoId: number | null;
  produtoNome: string | null;
  ultimoPrecoValido: number;
}

export interface ComparacaoImportacao {
  importacao: Importacao;
  totais: Record<Classificacao, number>;
  itens: ItemComparado[];
  ausentes: ItemAusente[];
}

export function sinalDoValor(valor: number): string {
  if (valor > 0) {
    return '+';
  }
  return valor < 0 ? '−' : '';
}
