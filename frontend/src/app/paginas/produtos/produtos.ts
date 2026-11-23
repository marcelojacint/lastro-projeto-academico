import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, of, switchMap } from 'rxjs';
import { Pagina } from '../../modelos/pagina';
import { FiltroProduto, Produto, StatusProduto } from '../../modelos/produto';
import { ProdutoService } from '../../servicos/produto-service';

export const ESPERA_DIGITACAO_MS = 300;

@Component({
  selector: 'app-produtos-pagina',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './produtos.html',
  styleUrl: './produtos.scss',
})
export class ProdutosPagina implements OnInit {
  private readonly construtor = inject(NonNullableFormBuilder);
  private readonly produtoService = inject(ProdutoService);
  private readonly consultas = new Subject<number>();

  protected readonly tamanhoPagina = 20;
  protected readonly colunas = [
    'codigo',
    'nome',
    'categoria',
    'status',
    'estoqueAtual',
    'estoqueMinimo',
    'precoVenda',
    'acoes',
  ];
  protected readonly statusDisponiveis: StatusProduto[] = ['ATIVO', 'INATIVO'];
  protected readonly pagina = signal<Pagina<Produto> | null>(null);
  protected readonly carregando = signal(true);

  protected readonly filtros = this.construtor.group({
    nome: '',
    categoria: '',
    status: this.construtor.control<StatusProduto | ''>(''),
  });

  constructor() {
    this.consultas
      .pipe(
        switchMap((indice) => {
          this.carregando.set(true);
          return this.produtoService
            .listar(this.filtros.getRawValue(), indice)
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        if (resultado) {
          this.pagina.set(resultado);
        }
        this.carregando.set(false);
      });

    this.filtros.valueChanges
      .pipe(debounceTime(ESPERA_DIGITACAO_MS), takeUntilDestroyed())
      .subscribe(() => this.consultas.next(0));
  }

  ngOnInit(): void {
    this.consultas.next(0);
  }

  protected mudarPagina(evento: PageEvent): void {
    this.consultas.next(evento.pageIndex);
  }

  protected rotuloStatus(status: StatusProduto): string {
    return status === 'ATIVO' ? 'Ativo' : 'Inativo';
  }

  protected mensagemListaVazia(): string {
    return possuiFiltro(this.filtros.getRawValue())
      ? 'Nenhum produto encontrado com os filtros informados.'
      : 'Nenhum produto cadastrado.';
  }
}

function possuiFiltro(filtro: FiltroProduto): boolean {
  return Object.values(filtro).some((valor) => valor.trim() !== '');
}
