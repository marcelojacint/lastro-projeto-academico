import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina } from '../modelos/pagina';
import { FiltroProduto, Produto, ProdutoRequisicao } from '../modelos/produto';

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/produtos';

  listar(filtro: FiltroProduto, pagina: number): Observable<Pagina<Produto>> {
    let parametros = new HttpParams().set('page', pagina);
    for (const [nome, valor] of Object.entries(filtro)) {
      const texto = valor.trim();
      if (texto !== '') {
        parametros = parametros.set(nome, texto);
      }
    }
    return this.http.get<Pagina<Produto>>(this.url, { params: parametros });
  }

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
