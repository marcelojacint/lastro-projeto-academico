export type LayoutPdf = 'TABELA_REFERENCIA' | 'ATACADO_VAREJO';

export const ROTULOS_LAYOUT: Record<LayoutPdf, string> = {
  TABELA_REFERENCIA: 'Tabela com referência',
  ATACADO_VAREJO: 'Atacado e varejo',
};

export interface FornecedorRequisicao {
  nome: string;
  cnpj: string | null;
  contato: string | null;
  layoutPdf: LayoutPdf;
  ativo: boolean;
}

export interface Fornecedor extends FornecedorRequisicao {
  id: number;
  criadoEm: string;
  atualizadoEm: string;
}
