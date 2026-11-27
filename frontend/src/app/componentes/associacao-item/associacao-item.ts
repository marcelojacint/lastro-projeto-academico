import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ErroApi } from '../../modelos/erro-api';
import {
  ImportacaoDetalhe,
  ItemConferencia,
  MODALIDADES_PRECO,
} from '../../modelos/item-importado';
import { Produto } from '../../modelos/produto';
import { ImportacaoService } from '../../servicos/importacao-service';
import { ProdutoService } from '../../servicos/produto-service';

export const ESPERA_BUSCA_PRODUTO_MS = 300;
const TAMANHO_MINIMO_BUSCA = 2;

export interface DadosAssociacaoItem {
  importacaoId: string;
  item: ItemConferencia;
}

export interface ProdutoAssociado {
  id: number;
  nome: string;
}

export interface ResultadoAssociacao {
  detalhe: ImportacaoDetalhe;
  produto: ProdutoAssociado;
}

@Component({
  selector: 'app-associacao-item',
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './associacao-item.html',
  styleUrl: './associacao-item.scss',
})
export class AssociacaoItemComponente {
  private readonly produtoService = inject(ProdutoService);
  private readonly importacaoService = inject(ImportacaoService);
  private readonly dialogo = inject(MatDialogRef<AssociacaoItemComponente, ResultadoAssociacao>);
  protected readonly dados = inject<DadosAssociacaoItem>(MAT_DIALOG_DATA);

  protected readonly busca = new FormControl<string | Produto>(this.dados.item.produtoNome ?? '', {
    nonNullable: true,
  });
  protected readonly formulario = new FormGroup({ busca: this.busca });
  protected readonly sugestoes = signal<Produto[]>([]);
  protected readonly escolhido = signal<Produto | null>(null);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly modalidade = this.dados.item.modalidadePreco
    ? MODALIDADES_PRECO[this.dados.item.modalidadePreco]
    : '—';

  constructor() {
    this.busca.valueChanges
      .pipe(
        map((valor) => (typeof valor === 'string' ? valor.trim() : null)),
        debounceTime(ESPERA_BUSCA_PRODUTO_MS),
        distinctUntilChanged(),
        switchMap((texto) => {
          if (texto === null) {
            return of(null);
          }
          this.escolhido.set(null);
          if (texto.length < TAMANHO_MINIMO_BUSCA) {
            return of([]);
          }
          return this.produtoService
            .listar({ nome: texto, categoria: '', status: 'ATIVO' }, 0)
            .pipe(
              map((pagina) => pagina.itens),
              catchError(() => of([])),
            );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((produtos) => {
        if (produtos !== null) {
          this.sugestoes.set(produtos);
        }
      });
  }

  protected nomeDoProduto(valor: string | Produto | null): string {
    return typeof valor === 'string' ? valor : (valor?.nome ?? '');
  }

  protected escolher(produto: Produto): void {
    this.escolhido.set(produto);
    this.erro.set(null);
  }

  protected associar(): void {
    const produto = this.escolhido();
    if (produto === null) {
      this.erro.set('Escolha um produto da lista');
      return;
    }
    this.salvando.set(true);
    this.importacaoService
      .associarItem(this.dados.importacaoId, this.dados.item.id, produto.id)
      .subscribe({
        next: (detalhe) =>
          this.dialogo.close({ detalhe, produto: { id: produto.id, nome: produto.nome } }),
        error: (falha: unknown) => {
          this.salvando.set(false);
          const corpo = falha instanceof HttpErrorResponse ? (falha.error as ErroApi | null) : null;
          this.erro.set(corpo?.erros?.[0]?.mensagem ?? 'Não foi possível associar o produto');
        },
      });
  }
}
