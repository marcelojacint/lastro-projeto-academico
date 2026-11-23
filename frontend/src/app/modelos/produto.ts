export type StatusProduto = 'ATIVO' | 'INATIVO';

export interface ProdutoRequisicao {
  codigo: string | null;
  nome: string;
  categoria: string | null;
  unidade: string | null;
  precoCompra: number | null;
  precoVenda: number;
  estoqueAtual: number | null;
  estoqueMinimo: number | null;
  status: StatusProduto;
}

export interface Produto extends ProdutoRequisicao {
  id: number;
  criadoEm: string;
  atualizadoEm: string;
}
