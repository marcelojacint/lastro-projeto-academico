import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { RotuloClassificacaoComponente } from '../../componentes/rotulo-classificacao/rotulo-classificacao';
import { StatusImportacaoComponente } from '../../componentes/status-importacao/status-importacao';
import {
  CLASSIFICACOES,
  Classificacao,
  ComparacaoImportacao,
  ORDEM_CLASSIFICACOES,
  sinalDoValor,
} from '../../modelos/comparacao';
import { MODALIDADES_PRECO, ModalidadePreco } from '../../modelos/item-importado';
import { ImportacaoService } from '../../servicos/importacao-service';

type FiltroClassificacao = Classificacao | 'TODAS';

interface OpcaoFiltro {
  valor: FiltroClassificacao;
  rotulo: string;
  quantidade: number;
}

@Component({
  selector: 'app-comparacao-pagina',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatTableModule,
    RotuloClassificacaoComponente,
    StatusImportacaoComponente,
  ],
  templateUrl: './comparacao.html',
  styleUrl: './comparacao.scss',
})
export class ComparacaoPagina implements OnInit {
  private readonly importacaoService = inject(ImportacaoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pedidos = new Subject<FiltroClassificacao>();

  readonly id = input.required<string>();

  protected readonly colunas = [
    'produto',
    'anterior',
    'atual',
    'diferenca',
    'variacao',
    'classificacao',
    'precoVenda',
  ];
  protected readonly comparacao = signal<ComparacaoImportacao | null>(null);
  protected readonly carregando = signal(true);
  protected readonly filtro = signal<FiltroClassificacao>('TODAS');

  protected readonly opcoesFiltro = computed<OpcaoFiltro[]>(() => {
    const totais = this.comparacao()?.totais;
    const quantidade = (classificacao: Classificacao) => totais?.[classificacao] ?? 0;
    return [
      {
        valor: 'TODAS',
        rotulo: 'Todos',
        quantidade: ORDEM_CLASSIFICACOES.reduce((soma, atual) => soma + quantidade(atual), 0),
      },
      ...ORDEM_CLASSIFICACOES.map((classificacao) => ({
        valor: classificacao,
        rotulo: CLASSIFICACOES[classificacao].rotulo,
        quantidade: quantidade(classificacao),
      })),
    ];
  });

  protected readonly ausentes = computed(() => this.comparacao()?.ausentes ?? []);

  ngOnInit(): void {
    this.pedidos
      .pipe(
        switchMap((filtro) => {
          this.carregando.set(true);
          return this.importacaoService
            .comparar(this.id(), filtro === 'TODAS' ? null : filtro)
            .pipe(
              catchError(() => {
                this.carregando.set(false);
                return EMPTY;
              }),
            );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((comparacao) => {
        this.comparacao.set(comparacao);
        this.carregando.set(false);
      });
    this.pedidos.next(this.filtro());
  }

  protected filtrar(valor: FiltroClassificacao | null | undefined): void {
    const filtro = valor ?? 'TODAS';
    this.filtro.set(filtro);
    this.pedidos.next(filtro);
  }

  protected sinal(valor: number): string {
    return sinalDoValor(valor);
  }

  protected absoluto(valor: number): number {
    return Math.abs(valor);
  }

  protected modalidade(modalidade: ModalidadePreco | null): string {
    return modalidade ? MODALIDADES_PRECO[modalidade] : '—';
  }
}
