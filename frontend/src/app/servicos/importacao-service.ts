import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Importacao } from '../modelos/importacao';

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
}
