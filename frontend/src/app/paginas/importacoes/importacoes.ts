import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { StatusImportacaoComponente } from '../../componentes/status-importacao/status-importacao';
import {
  AcaoImportacao,
  Importacao,
  MOTIVOS_FALHA,
  MotivoFalha,
  acaoDaImportacao,
} from '../../modelos/importacao';
import { ImportacaoService } from '../../servicos/importacao-service';

@Component({
  selector: 'app-importacoes-pagina',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    StatusImportacaoComponente,
  ],
  templateUrl: './importacoes.html',
  styleUrl: './importacoes.scss',
})
export class ImportacoesPagina implements OnInit {
  private readonly importacaoService = inject(ImportacaoService);

  protected readonly colunas = [
    'recebidaEm',
    'fornecedor',
    'arquivo',
    'totalItens',
    'status',
    'acao',
  ];
  protected readonly importacoes = signal<Importacao[]>([]);
  protected readonly carregando = signal(true);

  ngOnInit(): void {
    this.importacaoService.listar().subscribe({
      next: (importacoes) => {
        this.importacoes.set(importacoes);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  protected motivo(motivoFalha: MotivoFalha): string {
    return MOTIVOS_FALHA[motivoFalha];
  }

  protected acao(importacao: Importacao): AcaoImportacao | null {
    return acaoDaImportacao(importacao);
  }
}
