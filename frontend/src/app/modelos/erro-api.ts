export interface ErroCampo {
  campo: string | null;
  mensagem: string;
}

export interface ErroApi {
  status: number;
  detail?: string;
  erros?: ErroCampo[];
  importacaoId?: string;
}
