import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Importacao } from '../modelos/importacao';
import { CorrecaoItem, ImportacaoDetalhe } from '../modelos/item-importado';

@Injectable({ providedIn: 'root' })
export class ImportacaoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/importacoes';

  enviar(fornecedorId: number, arquivo: File): Observable<Importacao> {
    const formulario = new FormData();
    formulario.append('fornecedorId', String(fornecedorId));
    formulario.append('arquivo', arquivo, arquivo.name);
    return this.http.post<Importacao>(this.url, formulario);
  }

  listar(): Observable<Importacao[]> {
    return this.http.get<Importacao[]>(this.url);
  }

  detalhar(id: string): Observable<ImportacaoDetalhe> {
    return this.http.get<ImportacaoDetalhe>(`${this.url}/${id}`);
  }

  corrigirItem(id: string, itemId: number, correcao: CorrecaoItem): Observable<ImportacaoDetalhe> {
    return this.http.put<ImportacaoDetalhe>(`${this.url}/${id}/itens/${itemId}`, correcao);
  }

  associarItem(id: string, itemId: number, produtoId: number): Observable<ImportacaoDetalhe> {
    return this.http.post<ImportacaoDetalhe>(`${this.url}/${id}/itens/${itemId}/vinculo`, {
      produtoId,
    });
  }

  concluir(id: string): Observable<Importacao> {
    return this.http.post<Importacao>(`${this.url}/${id}/concluir`, null);
  }

  descartar(id: string): Observable<Importacao> {
    return this.http.post<Importacao>(`${this.url}/${id}/descartar`, null);
  }
}
