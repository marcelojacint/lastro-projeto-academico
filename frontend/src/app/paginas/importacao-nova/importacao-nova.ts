import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { StatusImportacaoComponente } from '../../componentes/status-importacao/status-importacao';
import { ErroApi } from '../../modelos/erro-api';
import { Fornecedor } from '../../modelos/fornecedor';
import { Importacao, acaoDaImportacao } from '../../modelos/importacao';
import {
  TAMANHO_MAXIMO_PDF_MB,
  erroDoArquivoPdf,
  formatarTamanho,
} from '../../servicos/arquivo-pdf';
import { FornecedorService } from '../../servicos/fornecedor-service';
import { ImportacaoService } from '../../servicos/importacao-service';

interface ImportacaoDuplicada {
  mensagem: string;
  rota: string;
}

@Component({
  selector: 'app-importacao-nova-pagina',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    StatusImportacaoComponente,
  ],
  templateUrl: './importacao-nova.html',
  styleUrl: './importacao-nova.scss',
})
export class ImportacaoNovaPagina implements OnInit {
  private readonly construtor = inject(NonNullableFormBuilder);
  private readonly fornecedorService = inject(FornecedorService);
  private readonly importacaoService = inject(ImportacaoService);

  protected readonly tamanhoMaximoMb = TAMANHO_MAXIMO_PDF_MB;
  protected readonly formatarTamanho = formatarTamanho;
  protected readonly fornecedoresAtivos = signal<Fornecedor[]>([]);
  protected readonly carregando = signal(true);
  protected readonly arquivo = signal<File | null>(null);
  protected readonly erroArquivo = signal<string | null>(null);
  protected readonly falhaRegistrada = signal(false);
  protected readonly duplicada = signal<ImportacaoDuplicada | null>(null);
  protected readonly enviando = signal(false);
  protected readonly recebida = signal<Importacao | null>(null);

  protected readonly formulario = this.construtor.group({
    fornecedorId: this.construtor.control<number | null>(null, Validators.required),
  });

  ngOnInit(): void {
    this.fornecedorService.listar().subscribe({
      next: (fornecedores) => {
        this.fornecedoresAtivos.set(fornecedores.filter((fornecedor) => fornecedor.ativo));
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  protected mensagemErroFornecedor(): string {
    const erros = this.formulario.controls.fornecedorId.errors;
    if (!erros) {
      return '';
    }
    return erros['required'] ? 'Escolha o fornecedor' : (erros['api'] ?? '');
  }

  protected aoEscolherArquivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const escolhido = entrada.files?.[0] ?? null;
    entrada.value = '';
    if (escolhido === null) {
      return;
    }
    this.arquivo.set(escolhido);
    this.erroArquivo.set(erroDoArquivoPdf(escolhido));
    this.falhaRegistrada.set(false);
    this.duplicada.set(null);
  }

  protected enviar(): void {
    this.formulario.markAllAsTouched();
    const erro = erroDoArquivoPdf(this.arquivo());
    this.erroArquivo.set(erro);
    const fornecedorId = this.formulario.controls.fornecedorId.value;
    const arquivo = this.arquivo();
    if (this.formulario.invalid || erro !== null || fornecedorId === null || arquivo === null) {
      return;
    }
    this.duplicada.set(null);
    this.enviando.set(true);
    this.importacaoService.enviar(fornecedorId, arquivo).subscribe({
      next: (importacao) => {
        this.enviando.set(false);
        this.recebida.set(importacao);
      },
      error: (falha: unknown) => {
        this.enviando.set(false);
        this.marcarErrosDaApi(falha);
      },
    });
  }

  protected enviarOutro(): void {
    this.recebida.set(null);
    this.arquivo.set(null);
    this.erroArquivo.set(null);
    this.falhaRegistrada.set(false);
    this.duplicada.set(null);
  }

  private marcarErrosDaApi(falha: unknown): void {
    if (!(falha instanceof HttpErrorResponse)) {
      return;
    }
    const corpo = falha.error as ErroApi | null;
    if (falha.status === 409 && corpo?.importacaoId) {
      this.mostrarDuplicada(corpo.importacaoId, corpo.detail);
      return;
    }
    this.falhaRegistrada.set(Boolean(corpo?.importacaoId));
    for (const item of corpo?.erros ?? []) {
      if (item.campo === 'arquivo') {
        this.erroArquivo.set(item.mensagem);
      }
      if (item.campo === 'fornecedorId') {
        this.formulario.controls.fornecedorId.setErrors({ api: item.mensagem });
      }
    }
  }

  private mostrarDuplicada(importacaoId: string, mensagem: string | undefined): void {
    this.duplicada.set({
      mensagem: mensagem ?? 'Este arquivo já foi importado para este fornecedor',
      rota: `/importacoes/${importacaoId}/conferencia`,
    });
    this.importacaoService.detalhar(importacaoId).subscribe({
      next: (detalhe) => {
        const acao = acaoDaImportacao(detalhe.importacao);
        const aviso = this.duplicada();
        if (acao && aviso) {
          this.duplicada.set({ ...aviso, rota: acao.rota });
        }
      },
    });
  }
}
