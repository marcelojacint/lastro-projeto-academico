import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Fornecedor, FornecedorRequisicao } from '../modelos/fornecedor';

@Injectable({ providedIn: 'root' })
export class FornecedorService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/fornecedores';

  listar(): Observable<Fornecedor[]> {
    return this.http.get<Fornecedor[]>(this.url);
  }

  buscar(id: number): Observable<Fornecedor> {
    return this.http.get<Fornecedor>(`${this.url}/${id}`);
  }

  cadastrar(fornecedor: FornecedorRequisicao): Observable<Fornecedor> {
    return this.http.post<Fornecedor>(this.url, fornecedor);
  }

  atualizar(id: number, fornecedor: FornecedorRequisicao): Observable<Fornecedor> {
    return this.http.put<Fornecedor>(`${this.url}/${id}`, fornecedor);
  }
}
