import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ErroApi } from '../../modelos/erro-api';
import {
  ImportacaoDetalhe,
  ItemConferencia,
  MODALIDADES_PRECO,
} from '../../modelos/item-importado';
import { ImportacaoService } from '../../servicos/importacao-service';
import { converterMoeda, formatarMoeda, validarMoeda } from '../../servicos/moeda';

export interface DadosCorrecaoItem {
  importacaoId: string;
  item: ItemConferencia;
}

type CampoCorrecao = 'referencia' | 'descricao' | 'unidade' | 'preco';

function precoPositivo(controle: AbstractControl<string>): ValidationErrors | null {
  const valor = converterMoeda(controle.value);
  return valor === 0 ? { zero: true } : null;
}

@Component({
  selector: 'app-correcao-item',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './correcao-item.html',
  styleUrl: './correcao-item.scss',
})
export class CorrecaoItemComponente {
  private readonly construtor = inject(NonNullableFormBuilder);
  private readonly importacaoService = inject(ImportacaoService);
  private readonly dialogo = inject(MatDialogRef<CorrecaoItemComponente, ImportacaoDetalhe>);
  protected readonly dados = inject<DadosCorrecaoItem>(MAT_DIALOG_DATA);

  protected readonly salvando = signal(false);
  protected readonly modalidade = this.dados.item.modalidadePreco
    ? MODALIDADES_PRECO[this.dados.item.modalidadePreco]
    : 'Não identificada';

  protected readonly formulario = this.construtor.group({
    referencia: [this.dados.item.referencia ?? '', Validators.maxLength(50)],
    descricao: [
      this.dados.item.descricao ?? '',
      [Validators.required, Validators.pattern(/\S/), Validators.maxLength(300)],
    ],
    unidade: [this.dados.item.unidade ?? '', Validators.maxLength(10)],
    preco: [
      formatarMoeda(this.dados.item.preco),
      [Validators.required, validarMoeda(), precoPositivo],
    ],
  });

  protected mensagemErro(campo: CampoCorrecao): string {
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
      return 'Use um valor como 35,00';
    }
    if (erros['negativo'] || erros['zero']) {
      return 'Informe um preço maior que zero';
    }
    return erros['api'] ?? '';
  }

  protected formatarPreco(): void {
    const controle = this.formulario.controls.preco;
    const valor = converterMoeda(controle.value);
    if (valor !== null && !Number.isNaN(valor)) {
      controle.setValue(formatarMoeda(valor));
    }
  }

  protected salvar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    this.salvando.set(true);
    this.importacaoService
      .corrigirItem(this.dados.importacaoId, this.dados.item.id, {
        referencia: valores.referencia.trim() || null,
        descricao: valores.descricao.trim(),
        unidade: valores.unidade.trim() || null,
        preco: converterMoeda(valores.preco),
      })
      .subscribe({
        next: (detalhe) => this.dialogo.close(detalhe),
        error: (falha: unknown) => {
          this.salvando.set(false);
          this.marcarErrosDaApi(falha);
        },
      });
  }

  private marcarErrosDaApi(falha: unknown): void {
    if (!(falha instanceof HttpErrorResponse)) {
      return;
    }
    const corpo = falha.error as ErroApi | null;
    for (const item of corpo?.erros ?? []) {
      const controle = item.campo ? this.formulario.get(item.campo) : null;
      if (controle) {
        controle.setErrors({ api: item.mensagem });
        controle.markAsTouched();
      }
    }
  }
}
