import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ErroApi } from '../../modelos/erro-api';
import {
  Fornecedor,
  FornecedorRequisicao,
  LayoutPdf,
  ROTULOS_LAYOUT,
} from '../../modelos/fornecedor';
import { digitosCnpj, formatarCnpj, validarCnpj } from '../../servicos/cnpj';
import { FornecedorService } from '../../servicos/fornecedor-service';

@Component({
  selector: 'app-fornecedores-pagina',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTableModule,
  ],
  templateUrl: './fornecedores.html',
  styleUrl: './fornecedores.scss',
})
export class FornecedoresPagina implements OnInit {
  private readonly construtor = inject(NonNullableFormBuilder);
  private readonly fornecedorService = inject(FornecedorService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly colunas = ['nome', 'cnpj', 'layoutPdf', 'status', 'acoes'];
  protected readonly layouts = Object.keys(ROTULOS_LAYOUT) as LayoutPdf[];
  protected readonly fornecedores = signal<Fornecedor[]>([]);
  protected readonly carregando = signal(true);
  protected readonly editando = signal<Fornecedor | null>(null);
  protected readonly salvando = signal(false);
  protected readonly formatarCnpj = formatarCnpj;

  protected readonly formulario = this.construtor.group({
    nome: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)]],
    cnpj: ['', validarCnpj()],
    contato: ['', Validators.maxLength(200)],
    layoutPdf: this.construtor.control<LayoutPdf | ''>('', Validators.required),
    ativo: true,
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected rotuloLayout(layout: LayoutPdf): string {
    return ROTULOS_LAYOUT[layout];
  }

  protected mensagemErro(campo: keyof typeof this.formulario.controls): string {
    const erros = this.formulario.controls[campo].errors;
    if (!erros) {
      return '';
    }
    if (erros['required'] || erros['pattern']) {
      return 'Campo obrigatório';
    }
    if (erros['maxlength']) {
      return `Use no máximo ${erros['maxlength'].requiredLength} caracteres`;
    }
    if (erros['cnpj']) {
      return 'CNPJ deve ter 14 dígitos';
    }
    return erros['api'] ?? '';
  }

  protected editar(fornecedor: Fornecedor): void {
    this.editando.set(fornecedor);
    this.formulario.reset({
      nome: fornecedor.nome,
      cnpj: formatarCnpj(fornecedor.cnpj),
      contato: fornecedor.contato ?? '',
      layoutPdf: fornecedor.layoutPdf,
      ativo: fornecedor.ativo,
    });
  }

  protected novo(): void {
    this.editando.set(null);
    this.formulario.reset();
  }

  protected salvar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const fornecedor = this.editando();
    const requisicao = this.montarRequisicao();
    const operacao =
      fornecedor === null
        ? this.fornecedorService.cadastrar(requisicao)
        : this.fornecedorService.atualizar(fornecedor.id, requisicao);
    operacao.subscribe({
      next: () => {
        this.salvando.set(false);
        this.snackBar.open(
          fornecedor === null ? 'Fornecedor cadastrado' : 'Fornecedor atualizado',
          'Fechar',
          { duration: 4000 },
        );
        this.novo();
        this.carregar();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.marcarErrosDaApi(erro);
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.fornecedorService.listar().subscribe({
      next: (fornecedores) => {
        this.fornecedores.set(fornecedores);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  private montarRequisicao(): FornecedorRequisicao {
    const valores = this.formulario.getRawValue();
    const contato = valores.contato.trim();
    return {
      nome: valores.nome.trim(),
      cnpj: digitosCnpj(valores.cnpj),
      contato: contato === '' ? null : contato,
      layoutPdf: valores.layoutPdf as LayoutPdf,
      ativo: this.editando() === null ? true : valores.ativo,
    };
  }

  private marcarErrosDaApi(erro: unknown): void {
    if (!(erro instanceof HttpErrorResponse)) {
      return;
    }
    const corpo = erro.error as ErroApi | null;
    for (const item of corpo?.erros ?? []) {
      const controle = item.campo ? this.formulario.get(item.campo) : null;
      if (controle) {
        controle.setErrors({ api: item.mensagem });
        controle.markAsTouched();
      }
    }
  }
}
