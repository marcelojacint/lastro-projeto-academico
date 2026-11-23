import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { ErroApi } from '../modelos/erro-api';

const DURACAO_MENSAGEM_MS = 8000;

export function mensagemDoErro(erro: HttpErrorResponse): string {
  if (erro.status === 0) {
    return 'Não foi possível conectar à API.';
  }
  const corpo = erro.error as ErroApi | null;
  const mensagens = corpo?.erros?.map((item) => item.mensagem) ?? [];
  if (mensagens.length > 0) {
    return mensagens.join(' · ');
  }
  return corpo?.detail ?? 'Erro inesperado ao falar com a API.';
}

export const erroApiInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const snackBar = inject(MatSnackBar);
  return proximo(requisicao).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse) {
        snackBar.open(mensagemDoErro(erro), 'Fechar', { duration: DURACAO_MENSAGEM_MS });
      }
      return throwError(() => erro);
    }),
  );
};
