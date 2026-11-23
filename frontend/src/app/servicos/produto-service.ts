import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Produto, ProdutoRequisicao } from '../modelos/produto';

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/produtos';

  buscar(id: number): Observable<Produto> {
    return this.http.get<Produto>(`${this.url}/${id}`);
  }

  cadastrar(produto: ProdutoRequisicao): Observable<Produto> {
    return this.http.post<Produto>(this.url, produto);
  }

  atualizar(id: number, produto: ProdutoRequisicao): Observable<Produto> {
    return this.http.put<Produto>(`${this.url}/${id}`, produto);
  }
}
