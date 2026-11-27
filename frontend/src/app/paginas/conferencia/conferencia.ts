import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import {
  CorrecaoItemComponente,
  DadosCorrecaoItem,
} from '../../componentes/correcao-item/correcao-item';
import { ErrosItemComponente } from '../../componentes/erros-item/erros-item';
import { EtiquetaStatusComponente } from '../../componentes/etiqueta-status/etiqueta-status';
import { StatusImportacaoComponente } from '../../componentes/status-importacao/status-importacao';
import { ApresentacaoStatus } from '../../modelos/importacao';
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
}
