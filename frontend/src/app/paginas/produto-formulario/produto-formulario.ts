import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ErroApi } from '../../modelos/erro-api';
import { Produto, ProdutoRequisicao, StatusProduto } from '../../modelos/produto';
import { converterMoeda, formatarMoeda, validarMoeda } from '../../servicos/moeda';
import { ProdutoService } from '../../servicos/produto-service';

type CampoMoeda = 'precoCompra' | 'precoVenda';

@Component({
  selector: 'app-produto-formulario-pagina',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './produto-formulario.html',
  styleUrl: './produto-formulario.scss',
})
export class ProdutoFormularioPagina implements OnInit {
  private readonly construtor = inject(NonNullableFormBuilder);
  private readonly produtoService = inject(ProdutoService);
  private readonly rota = inject(ActivatedRoute);
  private readonly roteador = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly id = signal<number | null>(null);
  protected readonly salvando = signal(false);
  protected readonly statusDisponiveis: StatusProduto[] = ['ATIVO', 'INATIVO'];

  protected readonly formulario = this.construtor.group({
    codigo: ['', Validators.maxLength(50)],
    nome: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)]],
    categoria: ['', Validators.maxLength(100)],
    unidade: ['', Validators.maxLength(20)],
    precoCompra: ['', validarMoeda()],
    precoVenda: ['', [Validators.required, validarMoeda()]],
    estoqueAtual: this.construtor.control<number | null>(0, Validators.min(0)),
    estoqueMinimo: this.construtor.control<number | null>(0, Validators.min(0)),
    status: this.construtor.control<StatusProduto>('ATIVO'),
  });

  ngOnInit(): void {
    const id = Number(this.rota.snapshot.paramMap.get('id'));
    if (Number.isInteger(id) && id > 0) {
      this.id.set(id);
      this.produtoService.buscar(id).subscribe((produto) => this.preencher(produto));
    }
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
    if (erros['moeda']) {
      return 'Valor inválido. Use o formato 1.234,56';
    }
    if (erros['negativo'] || erros['min']) {
      return 'O valor não pode ser negativo';
    }
    return erros['api'] ?? '';
  }

  protected formatarCampoMoeda(campo: CampoMoeda): void {
    const controle = this.formulario.controls[campo];
    const valor = converterMoeda(controle.value);
    if (controle.valid && valor !== null) {
      controle.setValue(formatarMoeda(valor));
    }
  }

  protected salvar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const id = this.id();
    const requisicao = this.montarRequisicao();
    const operacao =
      id === null
        ? this.produtoService.cadastrar(requisicao)
        : this.produtoService.atualizar(id, requisicao);
    operacao.subscribe({
      next: () => {
        this.snackBar.open(id === null ? 'Produto cadastrado' : 'Produto atualizado', 'Fechar', {
          duration: 4000,
        });
        this.roteador.navigate(['/produtos']);
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.marcarErrosDaApi(erro);
      },
    });
  }

  private preencher(produto: Produto): void {
    this.formulario.setValue({
      codigo: produto.codigo ?? '',
      nome: produto.nome,
      categoria: produto.categoria ?? '',
      unidade: produto.unidade ?? '',
      precoCompra: formatarMoeda(produto.precoCompra),
      precoVenda: formatarMoeda(produto.precoVenda),
      estoqueAtual: produto.estoqueAtual,
      estoqueMinimo: produto.estoqueMinimo,
      status: produto.status,
    });
  }

  private montarRequisicao(): ProdutoRequisicao {
    const valores = this.formulario.getRawValue();
    return {
      codigo: textoOuNulo(valores.codigo),
      nome: valores.nome.trim(),
      categoria: textoOuNulo(valores.categoria),
      unidade: textoOuNulo(valores.unidade),
      precoCompra: converterMoeda(valores.precoCompra),
      precoVenda: converterMoeda(valores.precoVenda) ?? 0,
      estoqueAtual: valores.estoqueAtual,
      estoqueMinimo: valores.estoqueMinimo,
      status: valores.status,
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

function textoOuNulo(texto: string): string | null {
  const limpo = texto.trim();
  return limpo === '' ? null : limpo;
}
