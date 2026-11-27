import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { Observable, concatMap, filter, from, switchMap } from 'rxjs';
import {
  AssociacaoItemComponente,
  DadosAssociacaoItem,
  ProdutoAssociado,
  ResultadoAssociacao,
} from '../../componentes/associacao-item/associacao-item';
import {
  CorrecaoItemComponente,
  DadosCorrecaoItem,
} from '../../componentes/correcao-item/correcao-item';
import { ConfirmacaoComponente, DadosConfirmacao } from '../../componentes/confirmacao/confirmacao';
import { ErrosItemComponente } from '../../componentes/erros-item/erros-item';
import { EtiquetaStatusComponente } from '../../componentes/etiqueta-status/etiqueta-status';
import { StatusImportacaoComponente } from '../../componentes/status-importacao/status-importacao';
import { ApresentacaoStatus, Importacao } from '../../modelos/importacao';
import {
  ImportacaoDetalhe,
  ItemConferencia,
  MODALIDADES_PRECO,
  ModalidadePreco,
  SITUACOES_ITEM,
  SituacaoItem,
} from '../../modelos/item-importado';
import { ImportacaoService } from '../../servicos/importacao-service';

type FiltroSituacao = SituacaoItem | 'TODAS';

interface OfertaMesmaLinha {
  numeroLinha: number;
  itens: ItemConferencia[];
  produto: ProdutoAssociado;
}

interface OpcaoFiltro {
  valor: FiltroSituacao;
  rotulo: string;
  quantidade: number;
}

@Component({
  selector: 'app-conferencia-pagina',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatTableModule,
    ErrosItemComponente,
    EtiquetaStatusComponente,
    StatusImportacaoComponente,
  ],
  templateUrl: './conferencia.html',
  styleUrl: './conferencia.scss',
})
export class ConferenciaPagina implements OnInit {
  private readonly importacaoService = inject(ImportacaoService);
  private readonly dialogo = inject(MatDialog);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly colunas = [
    'linha',
    'referencia',
    'descricao',
    'unidade',
    'modalidade',
    'quantidade',
    'preco',
    'situacao',
    'produto',
    'erros',
    'acao',
  ];
  protected readonly detalhe = signal<ImportacaoDetalhe | null>(null);
  protected readonly carregando = signal(true);
  protected readonly filtro = signal<FiltroSituacao>('TODAS');
  protected readonly oferta = signal<OfertaMesmaLinha | null>(null);
  protected readonly associandoOferta = signal(false);
  protected readonly finalizando = signal(false);

  protected readonly emConferencia = computed(
    () => this.detalhe()?.importacao.status === 'EM_CONFERENCIA',
  );

  protected readonly opcoesFiltro = computed<OpcaoFiltro[]>(() => {
    const itens = this.detalhe()?.itens ?? [];
    const contar = (situacao: SituacaoItem) =>
      itens.filter((item) => item.situacao === situacao).length;
    return [
      { valor: 'TODAS', rotulo: 'Todos', quantidade: itens.length },
      { valor: 'VALIDO', rotulo: 'Válidos', quantidade: contar('VALIDO') },
      {
        valor: 'PENDENTE_ASSOCIACAO',
        rotulo: 'Pendentes de associação',
        quantidade: contar('PENDENTE_ASSOCIACAO'),
      },
      { valor: 'INVALIDO', rotulo: 'Inválidos', quantidade: contar('INVALIDO') },
    ];
  });

  protected readonly itensVisiveis = computed(() => {
    const itens = this.detalhe()?.itens ?? [];
    const filtro = this.filtro();
    return filtro === 'TODAS' ? itens : itens.filter((item) => item.situacao === filtro);
  });

  ngOnInit(): void {
    this.importacaoService.detalhar(this.id()).subscribe({
      next: (detalhe) => {
        this.detalhe.set(detalhe);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  protected filtrar(valor: FiltroSituacao | null | undefined): void {
    this.filtro.set(valor ?? 'TODAS');
  }

  protected situacao(item: ItemConferencia): ApresentacaoStatus {
    return SITUACOES_ITEM[item.situacao];
  }

  protected modalidade(modalidade: ModalidadePreco | null): string {
    return modalidade ? MODALIDADES_PRECO[modalidade] : '—';
  }

  protected corrigir(item: ItemConferencia): void {
    const dados: DadosCorrecaoItem = { importacaoId: this.id(), item };
    this.dialogo
      .open<CorrecaoItemComponente, DadosCorrecaoItem, ImportacaoDetalhe>(CorrecaoItemComponente, {
        data: dados,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
      })
      .afterClosed()
      .subscribe((detalhe) => {
        if (detalhe) {
          this.detalhe.set(detalhe);
        }
      });
  }

  protected associar(item: ItemConferencia): void {
    const dados: DadosAssociacaoItem = { importacaoId: this.id(), item };
    this.dialogo
      .open<AssociacaoItemComponente, DadosAssociacaoItem, ResultadoAssociacao>(
        AssociacaoItemComponente,
        { data: dados, autoFocus: 'first-tabbable', restoreFocus: true },
      )
      .afterClosed()
      .subscribe((resultado) => {
        if (!resultado) {
          return;
        }
        this.detalhe.set(resultado.detalhe);
        const mesmaLinha = resultado.detalhe.itens.filter(
          (outro) =>
            outro.numeroLinha === item.numeroLinha &&
            outro.id !== item.id &&
            outro.apresentacaoId !== null &&
            outro.produtoId === null,
        );
        this.oferta.set(
          mesmaLinha.length > 0
            ? { numeroLinha: item.numeroLinha, itens: mesmaLinha, produto: resultado.produto }
            : null,
        );
      });
  }

  protected modalidadesDaOferta(oferta: OfertaMesmaLinha): string {
    return oferta.itens.map((outro) => this.modalidade(outro.modalidadePreco)).join(' e ');
  }

  protected aceitarOferta(): void {
    const oferta = this.oferta();
    if (oferta === null) {
      return;
    }
    this.associandoOferta.set(true);
    from(oferta.itens)
      .pipe(
        concatMap((outro) =>
          this.importacaoService.associarItem(this.id(), outro.id, oferta.produto.id),
        ),
      )
      .subscribe({
        next: (detalhe) => this.detalhe.set(detalhe),
        complete: () => {
          this.associandoOferta.set(false);
          this.oferta.set(null);
        },
        error: () => this.associandoOferta.set(false),
      });
  }

  protected recusarOferta(): void {
    this.oferta.set(null);
  }

  protected concluir(): void {
    const importacao = this.detalhe()?.importacao;
    if (!importacao) {
      return;
    }
    const mensagens = [
      `${this.quantidade(importacao.itensValidos, 'preço válido vai', 'preços válidos vão')} para o histórico. Os preços já gravados não mudam.`,
    ];
    if (importacao.itensPendentes > 0) {
      mensagens.push(
        `${this.quantidade(importacao.itensPendentes, 'item pendente ou inválido fica', 'itens pendentes ou inválidos ficam')} de fora, e a importação será concluída com pendências.`,
      );
    }
    this.finalizar(
      { titulo: 'Concluir importação?', mensagens, confirmar: 'Concluir' },
      () => this.importacaoService.concluir(this.id()),
      () => void this.router.navigate(['/importacoes', this.id(), 'comparacao']),
    );
  }

  protected descartar(): void {
    this.finalizar(
      {
        titulo: 'Descartar importação?',
        mensagens: [
          'Nenhum preço desta importação vai para o histórico.',
          'A importação não poderá mais ser conferida nem concluída.',
        ],
        confirmar: 'Descartar',
      },
      () => this.importacaoService.descartar(this.id()),
      (importacao) => this.detalhe.update((atual) => (atual ? { ...atual, importacao } : atual)),
    );
  }

  private finalizar(
    dados: DadosConfirmacao,
    acao: () => Observable<Importacao>,
    depois: (importacao: Importacao) => void,
  ): void {
    this.dialogo
      .open<ConfirmacaoComponente, DadosConfirmacao, boolean>(ConfirmacaoComponente, {
        data: dados,
        restoreFocus: true,
      })
      .afterClosed()
      .pipe(
        filter((confirmado) => confirmado === true),
        switchMap(() => {
          this.finalizando.set(true);
          return acao();
        }),
      )
      .subscribe({
        next: (importacao) => {
          this.finalizando.set(false);
          this.oferta.set(null);
          depois(importacao);
        },
        error: () => this.finalizando.set(false),
      });
  }

  private quantidade(valor: number, singular: string, plural: string): string {
    return `${valor} ${valor === 1 ? singular : plural}`;
  }
}
